# Search platform

Full-text search, facets and typo-tolerant matching are served by **Meilisearch**, behind the
engine-neutral contracts in `shared.search`. PostgreSQL stays the source of truth: every index is
a disposable projection that can be rebuilt from its tables at any time.

- **Owning modules define their own documents.** Each index has exactly one `SearchDocumentSource`
  bean, and it lives in the module that owns the data. No module imports a Meilisearch type.
- **The `search` module is a leaf.** It holds the engine adapter and the sync machinery and knows
  nothing about any domain. Nothing depends on it.
- **Off by default.** With `SEARCH_ENABLED=false` nothing is indexed, no HTTP call is made, the
  health endpoint has no `search` component, and `SearchGateway` throws
  `SearchUnavailableException`, so callers fall back to the database.

## Flow

```
 UI                        API (owning module)                    storage
 ──                        ───────────────────                    ───────
 GET /courses?q=java&...
        │
        ▼
 Controller ──► Service ── SearchAvailability.isReadEnabled("courses")?
                   │            │ no / SearchUnavailableException
                   │            └──────────────► JPA Specification ──► PostgreSQL
                   │ yes
                   ▼
            SearchRequest(index, text, SearchParamsTranslator.toFilter(params),
                          scope = CourseSearchScopes.forCaller(...))
                   │
                   ▼
            SearchGateway.search ─────────────────────────────────► Meilisearch
            (scope AND filter, quoted and escaped)                   (index "courses")
                   │
                   ▼
            SearchPage { hits[uuid, document, formatted], total_hits, facets }


 Write path (no search code in the service):

 POST /courses/{uuid} ──► Service ──► repository.save(course) ─► PostgreSQL
                                          │ JPA @PostUpdate
                                          ▼
                          SearchIndexingEntityListener (trigger lookup, no queries)
                                          │ enqueue("courses", uuid)
                                          ▼
                          SearchIndexRequests (bound to the transaction)
                                          │ before commit: flush, publish SearchIndexRequested
                                          ▼
                          event_publication row  ◄── same transaction as the change
                                          │ after commit, @Async on a single thread
                                          ▼
                          SearchIndexingListener ─► source.loadByUuids ─► PostgreSQL
                                          │ upsert returned / delete missing
                                          ▼
                                     Meilisearch (waits for the task to succeed;
                                     on failure the publication stays incomplete
                                     and is resubmitted every 5 minutes)
```

## Configuration

| Property | Env var | Default | Meaning |
|---|---|---|---|
| `search.enabled` | `SEARCH_ENABLED` | `false` | Master switch for indexing and search |
| `search.read-enabled.<index>` | `SEARCH_READENABLED_<INDEX>` | `false` | Route reads for this index to search. Indexing runs whenever search is enabled; reads are opted in separately |
| `search.auto-rebuild` | `SEARCH_AUTO_REBUILD` | `false` | Rebuild on startup when a definition's `schemaVersion` moved on |
| `search.meilisearch.host` | `SEARCH_MEILISEARCH_HOST` | `http://meilisearch:7700` | Engine URL (internal Docker network only) |
| `search.meilisearch.api-key` | `SEARCH_MEILISEARCH_API_KEY` | empty | A **scoped** key, never the master key |
| `search.meilisearch.connect-timeout` / `read-timeout` | `SEARCH_MEILISEARCH_CONNECT_TIMEOUT` / `_READ_TIMEOUT` | `PT2S` / `PT5S` | HTTP timeouts |
| `search.meilisearch.task-wait-timeout` | `SEARCH_MEILISEARCH_TASK_WAIT_TIMEOUT` | `PT30S` | How long a write waits for its engine task |
| `search.reconcile-interval` | `SEARCH_RECONCILE_INTERVAL` | `PT1H` | Drift check (engine count vs `countIndexable()`) |
| `search.resubmit-interval` / `resubmit-older-than` | `SEARCH_RESUBMIT_INTERVAL` / `_OLDER_THAN` | `PT5M` / `PT5M` | Retry of failed syncs |
| `search.rebuild-batch-size` | `SEARCH_REBUILD_BATCH_SIZE` | `500` | Rows per rebuild batch |

`docker/compose.yaml` runs `getmeili/meilisearch` (pinned tag, no published port, 512 MB limit)
with `MEILI_MASTER_KEY` from `docker/.env`. Create the application key once with the master key:

```bash
curl -X POST http://meilisearch:7700/keys -H "Authorization: Bearer $MEILI_MASTER_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"description":"elimika api","actions":["search","documents.*","indexes.*","settings.*","tasks.get","stats.get"],"indexes":["*"],"expiresAt":null}'
```

Put the returned `key` in `SEARCH_MEILISEARCH_API_KEY`.

## Admin endpoints

Platform admins only (`@domainSecurityService.isPlatformAdmin()`), registered only when search is
enabled. Write endpoints answer **202** and run in the background.

| Method | Path | Does |
|---|---|---|
| `GET` | `/api/v1/admin/search/indexes` | Each index: definition version, `search_index_state` row, live engine stats |
| `POST` | `/api/v1/admin/search/indexes/{index}/rebuild` | Blue/green rebuild of one index |
| `POST` | `/api/v1/admin/search/rebuild?module=course` | Rebuild every index, or those of one module (from the source's package) |
| `POST` | `/api/v1/admin/search/indexes/{index}/documents/{uuid}/sync` | Reload one document from its source |

A rebuild creates `<index>__build_<yyyyMMddHHmm>`, applies the settings, copies the source in keyset
batches (checkpointed in `search_index_state`, so a restart resumes it), swaps it with the live index
and drops the old contents. While it runs, live writes go to both indexes.

## Adding a searchable entity

Everything below lives in the **owning module** (e.g. `course/search/`). Nothing is added to the
`search` module.

### 1. The document record

A flat, denormalised projection. Snake-case `@JsonProperty` names are the attribute names.
Store instants as UTC epoch seconds (`*_at` longs) — the engine range-compares numbers only, and
`SearchParamsTranslator` turns ISO dates in requests into epoch seconds. Include every attribute
that a **scope** needs (organisation, visibility, owner ids).

```java
public record CourseSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("status") String status,
        @JsonProperty("organisation_uuid") UUID organisationUuid,
        @JsonProperty("category_uuids") List<UUID> categoryUuids,
        @JsonProperty("published_at") Long publishedAt
) implements SearchDocument { }
```

### 2. The `SearchDocumentSource`

```java
@Component
class CourseSearchSource implements SearchDocumentSource<CourseSearchDocument> {
    static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of("courses", 1,
            List.of("title", "description"),                                // searchable, by priority
            List.of("status", "organisation_uuid", "category_uuids", "published_at"), // filterable allow-list
            List.of("title", "published_at"));                              // sortable allow-list

    public SearchIndexDefinition definition() { return DEFINITION; }

    // Only documents that SHOULD be indexed; a UUID left out is deleted from the index.
    public List<CourseSearchDocument> loadByUuids(Collection<UUID> uuids) { ... }

    // Keyset scan for rebuilds: rows with id > lastId, ordered by id; return SearchBatch.end(lastId) when done.
    public SearchBatch<CourseSearchDocument> loadAfter(long lastId, int batchSize) { ... }

    public long countIndexable() { return repository.countIndexable(); } // optional, enables drift checks
    ...
}
```

Bump `schemaVersion` whenever the document shape or settings change, then rebuild (or enable
`search.auto-rebuild`).

### 3. Triggers

Declare which entity writes affect the index. Trigger functions run inside Hibernate's flush: read
only fields already on the entity — no queries, no lazy collections.

```java
public List<SearchIndexTrigger<?>> triggers() {
    return List.of(
            SearchIndexTrigger.direct(Course.class, Course::getUuid),                  // the row itself
            SearchIndexTrigger.direct(CourseRequirement.class, CourseRequirement::getCourseUuid), // an embedded child
            SearchIndexTrigger.fanOut(CourseCategory.class, c -> "category:" + c.getUuid()));    // many documents
}

// Fan-out keys are resolved after commit, where queries are fine.
public Set<UUID> resolveFanOut(String key) { ... courses in that category ... }
```

Writes that bypass JPA (bulk `@Modifying` queries, `JdbcTemplate`) do not fire triggers; call
`SearchIndexRequests.enqueue(index, uuids)` yourself in the same transaction.

### 4. The scope factory

One class that turns the caller into a `SearchScope`, mirroring the rules the module's database
queries already apply. There is no default scope: `SearchScope.unrestricted("platform-admin")` must
be written out explicitly.

```java
final class CourseSearchScopes {
    static SearchScope forCaller(Caller caller) {
        if (caller.isPlatformAdmin()) return SearchScope.unrestricted("platform-admin");
        return SearchScope.of(SearchFilter.or(
                SearchFilter.eq("status", "published"),
                SearchFilter.eq("organisation_uuid", caller.organisationUuid())), "org:" + caller.organisationUuid());
    }
}
```

### 5. `q` routing

Keep the existing endpoint and parameters; route only requests with `q` when the index is read-enabled,
and fall back on `SearchUnavailableException`.

```java
if (StringUtils.hasText(q) && searchAvailability.isReadEnabled("courses")) {
    try {
        SearchRequest request = new SearchRequest("courses", q,
                SearchParamsTranslator.toFilter(searchParams, DEFINITION),   // unknown key -> 400
                CourseSearchScopes.forCaller(caller),
                SearchParamsTranslator.toSort(searchParams.get("sort"), DEFINITION),
                page, size, List.of(), null);
        return toResponse(searchGateway.search(request));
    } catch (SearchUnavailableException ex) {
        log.warn("Search unavailable, falling back to the database: {}", ex.getMessage());
    }
}
return databaseQuery(searchParams, pageable);
```

Then enable indexing (`SEARCH_ENABLED=true`), rebuild the index from the admin endpoint, check it in
`GET /api/v1/admin/search/indexes`, and only then set `SEARCH_READENABLED_COURSES=true`.
