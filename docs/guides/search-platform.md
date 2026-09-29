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

 Global search (no hydration, one engine round trip):

 GET /api/v1/search?q=astro&types=courses,people&limit=5      (permitAll; anonymous allowed)
        │
        ▼
 GlobalSearchController ──► GlobalSearchService (search module)
                                │ validate q (≥ 2 chars), limit (1-20), types (unknown → 400)
                                │ search.enabled? no → 503
                                │ for each requested type, in order:
                                │   index read-enabled?               no → skip
                                │   provider.scopeForCurrentCaller()   empty → skip (e.g. people for a student)
                                ▼
            GlobalSearchProvider beans, one per index, in the OWNING module
            (course/internal/search, classes/search, instructor/search, tenancy/search)
            scope = the same rule as the module's own listing; searchOn narrows people to names
                                │
                                ▼
            SearchGateway.multiSearchPerIndex ── one POST /multi-search ──► Meilisearch
                                │ one page per type, each with its own total
                                ▼
            provider.toHit(hit) → GlobalSearchHit { type, uuid, title, subtitle, image_url, highlight }
                                │ built from stored document fields, no PostgreSQL query
                                ▼
 ApiResponse { hits: [...grouped by type, in request order], totals: { courses: 12, people: 3 } }
```

## Configuration

| Property | Env var | Default | Meaning |
|---|---|---|---|
| `search.enabled` | `SEARCH_ENABLED` | `false` | Master switch for indexing and search |
| `search.read-enabled.<index>` | `SEARCH_READENABLED_<INDEX>` | `false` | Route reads for this index to search - both the module's `q` listings and global search. Indexing runs whenever search is enabled; reads are opted in separately |
| `search.auto-rebuild` | `SEARCH_AUTO_REBUILD` | `false` | Rebuild on startup when a definition's `schemaVersion` moved on |
| `search.full-rebuild-cron` | `SEARCH_FULL_REBUILD_CRON` | `0 30 1 * * *` | Nightly blue/green rebuild of every index, UTC. `-` disables it |
| `search.meilisearch.host` | `SEARCH_MEILISEARCH_HOST` | `http://meilisearch:7700` | Engine URL (internal Docker network only) |
| `search.meilisearch.api-key` | `SEARCH_MEILISEARCH_API_KEY` | empty | A **scoped** key, never the master key |
| `search.meilisearch.connect-timeout` / `read-timeout` | `SEARCH_MEILISEARCH_CONNECT_TIMEOUT` / `_READ_TIMEOUT` | `PT2S` / `PT5S` | HTTP timeouts |
| `search.meilisearch.task-wait-timeout` | `SEARCH_MEILISEARCH_TASK_WAIT_TIMEOUT` | `PT30S` | How long a write waits for its engine task |
| `search.reconcile-interval` | `SEARCH_RECONCILE_INTERVAL` | `PT1H` | Drift check (engine count vs `countIndexable()`) |
| `search.resubmit-interval` / `resubmit-older-than` | `SEARCH_RESUBMIT_INTERVAL` / `_OLDER_THAN` | `PT5M` / `PT5M` | Retry of failed syncs |
| `search.rebuild-batch-size` | `SEARCH_REBUILD_BATCH_SIZE` | `500` | Rows per rebuild batch |

The per-index read flags, one per index:

| Index | Read flag |
|---|---|
| `courses` | `SEARCH_READENABLED_COURSES` |
| `programs` | `SEARCH_READENABLED_PROGRAMS` |
| `rubrics` | `SEARCH_READENABLED_RUBRICS` |
| `classes` | `SEARCH_READENABLED_CLASSES` |
| `marketplace_jobs` | `SEARCH_READENABLED_MARKETPLACE_JOBS` |
| `instructors` | `SEARCH_READENABLED_INSTRUCTORS` |
| `organisations` | `SEARCH_READENABLED_ORGANISATIONS` |
| `people` | `SEARCH_READENABLED_PEOPLE` |

`docker/compose.yaml` runs `getmeili/meilisearch` (pinned tag, no published port, 512 MB limit)
with `MEILI_MASTER_KEY` from `docker/.env`. The application key is created once with the master key
(see the runbook below).

## Global search endpoints

Registered whether or not search is on, so a disabled engine answers **503** rather than 404.
`GET` on both routes is `permitAll` in `SecurityConfiguration`: each type's provider decides what the
caller sees, and `DomainSecurityService` answers "no user, no roles" for an anonymous request, so every
provider falls back to its public boundary or hides the type.

### `GET /api/v1/search?q=&types=&limit=`

| Param | Rule |
|---|---|
| `q` | required, 2-200 characters after trimming, else 400 |
| `types` | comma-separated; omitted means all eight in the order `courses, programs, classes, marketplace_jobs, instructors, organisations, people, rubrics`. An unknown type is a 400; a type the caller may not see, or whose index is not read-enabled, is skipped silently |
| `limit` | hits per type, 1-20, default 5, else 400 |

```json
{
  "success": true,
  "data": {
    "hits": [
      { "type": "courses", "uuid": "…", "title": "Astronomy 101", "subtitle": "Ada Creator",
        "image_url": "/api/v1/files/…", "highlight": "<em>Astro</em>nomy 101" },
      { "type": "people", "uuid": "…", "title": "Asha Otieno", "subtitle": "student", "image_url": null,
        "highlight": "<em>Asha</em> Otieno" }
    ],
    "totals": { "courses": 12, "people": 3 }
  }
}
```

- **Grouped, not merged.** Hits are grouped per type in the requested order, each group in relevance
  order. Relevance scores from indexes with different attributes and ranking rules are not comparable,
  so a merged (federated) ranking would interleave types arbitrarily; the UI renders a sectioned
  dropdown anyway, and grouping gives an exact `totals` entry per type for "see all (12)".
- **One round trip.** All allowed types go to Meilisearch in a single non-federated `/multi-search`.
- **No hydration.** `GlobalSearchHit` is built from stored document fields by the owning module's
  provider, so a change reaches global search within seconds (the async indexing delay), and a stale
  denormalised name stays until the nightly rebuild (see the staleness column below).
- **503** when `search.enabled=false`, when none of the requested types is read-enabled, or when the
  engine fails: `{"success": false, "message": "Search is unavailable", "error": "…try again later"}`.
- **Privacy.** The query text is never logged by global search (only the types and its length), and
  with `people` among the types an engine error is logged without its message.

### `GET /api/v1/search/{type}?q=&<field_op>=&facets=&sort=&page=&size=`

A "see all results" page for one type: `{ content: [GlobalSearchHit], metadata: PageMetadata, facets: { attribute: { value: count } } }`.

- `q` is optional (2-200 characters when present); without it the page lists by filter and sort.
- Filters use the `field` / `field_op` vocabulary (`eq, noteq, in, notin, gt, gte, lt, lte, between`),
  translated by `SearchParamsTranslator` against the index's filterable allow-list; `facets` must name
  filterable attributes; `sort` is `field[,asc|desc]` over sortable attributes. Anything else is a 400.
- `page` ≥ 0, `size` 1-100 (default 20).
- **403** when the provider returns no scope for the caller (for example `people` for a student).
  **503** when search, or this type's index, is not enabled.

### Who sees what

| Type | Anonymous | Signed-in user | Platform admin |
|---|---|---|---|
| `courses` | published, approved, active | + own (creator) and enrolled / manageable courses | all |
| `programs` | live (published, approved, active) | + authored under own creator or instructor identity | all |
| `classes` | active `PUBLIC` classes with approved content | + classes of staffed organisations, taught, enrolled | all |
| `organisations` | active and verified | active and verified | all, including pending |
| `marketplace_jobs` | hidden | `OPEN` jobs + all jobs of staffed organisations | all |
| `instructors` | hidden | verified + own profile | all |
| `people` | hidden | organisation managers only: active members of managed organisations, **names only** (`searchOn` = `full_name`, `first_name`, `last_name`; highlight from `full_name`) | everyone, every searchable attribute |
| `rubrics` | hidden | course creators: public + own; instructors: public and active; others hidden | all |

## Admin endpoints

Platform admins only (`@domainSecurityService.isPlatformAdmin()`), registered only when search is
enabled. Write endpoints answer **202** and run in the background.

| Method | Path | Does |
|---|---|---|
| `GET` | `/api/v1/admin/search/indexes` | Each index: definition version, `search_index_state` row, read flag, live engine stats |
| `POST` | `/api/v1/admin/search/indexes/{index}/rebuild` | Blue/green rebuild of one index |
| `POST` | `/api/v1/admin/search/rebuild?module=course` | Rebuild every index, or those of one module (from the source's package) |
| `POST` | `/api/v1/admin/search/indexes/{index}/documents/{uuid}/sync` | Reload one document from its source |

A rebuild creates `<index>__build_<yyyyMMddHHmm>`, applies the settings, copies the source in keyset
batches (checkpointed in `search_index_state`, so a restart resumes it), swaps it with the live index
and drops the old contents. While it runs, live writes go to both indexes.

The same rebuild runs for every index each night at `search.full-rebuild-cron` (01:30 UTC by
default). An index already `REBUILDING` is skipped; the rest are queued on the rebuilder's single
thread and run one after another.

## Indexes

| Index | Owner (source, scopes, global provider) | Source table | Module read endpoints (`q`) |
|---|---|---|---|
| `courses` | `course/internal/search`: `CourseSearchSource`, `CatalogueSearchScopes.courses`, `CatalogueGlobalSearchProviders.Courses` | `courses` (root courses only) | `GET /api/v1/courses`, `/courses/search` and the catalogue listings |
| `programs` | `course/internal/search`: `ProgramSearchSource`, `CatalogueSearchScopes.programs`, `…Programs` | `training_programs` | `GET /api/v1/programs`, `/programs/search` |
| `rubrics` | `course/internal/search`: `RubricSearchSource`, `CatalogueSearchScopes.rubrics` / `publicRubrics`, `…Rubrics` | `assessment_rubrics` | `GET /api/v1/rubrics/search`, `/rubrics/discovery/search` |
| `classes` | `classes/search`: `ClassSearchSource`, `ClassSearchScopes`, `ClassesGlobalSearchProviders.Classes` | `class_definitions` | `GET /api/v1/classes`, `/classes/active`, `/classes/organisation/{uuid}` |
| `marketplace_jobs` | `classes/search`: `MarketplaceJobSearchSource`, `MarketplaceJobSearchScopes`, `…MarketplaceJobs` | `class_marketplace_jobs` | `GET /api/v1/classes/jobs` |
| `instructors` | `instructor/search`: `InstructorSearchSource`, `InstructorSearchScopes` + `InstructorVisibility`, `InstructorGlobalSearchProvider` | `instructors` | `GET /api/v1/instructors`, `/instructors/search` |
| `organisations` | `tenancy/search`: `OrganisationSearchSource`, `OrganisationSearchScopes`, `TenancyGlobalSearchProviders.Organisations` | `organisation` | `GET /api/v1/organisations`, the pending-verification queue |
| `people` | `tenancy/search`: `PeopleSearchSource`, `PeopleSearchScopes`, `TenancyGlobalSearchProviders.People` | `users` | `/users/search`, `/admin/users/eligible`, organisation rosters |

### Fields, scopes, triggers and staleness

| Index | Searchable (priority order) | Filterable | Sortable | Scope (non-admin) | Re-indexed by | Known staleness |
|---|---|---|---|---|---|---|
| `courses` | name, category_names, creator_name, difficulty_name, description, objectives | status, active, admin_approved, is_public, course_creator_uuid, category_uuids, difficulty_uuid, is_free, price, uuid, created_at | name, created_at, price, rating_avg, enrolment_count | `is_public` OR own creator OR related (enrolled / manageable) course | `Course`, `CourseCategoryMapping`, `CourseReview`; fan-out on `Category`, `DifficultyLevel` | `creator_name` (course-creator module) until the nightly rebuild |
| `programs` | title, course_names, category_name, creator_name, description | status, is_published, admin_approved, active, is_public, course_creator_uuid, … | title, created_at | `is_public` OR authored under own identities | `TrainingProgram`, `ProgramCourse`; fan-out on `Course`, `Category` | `creator_name` until the nightly rebuild |
| `rubrics` | title, rubric_type, description | is_public, is_active, status, course_creator_uuid, rubric_type, usage_count, uuid, created_at | title, created_at, usage_count | public OR own; discovery: public AND active | `AssessmentRubric`, `CourseRubricAssociation` | none |
| `classes` | title, course_name, program_title, organisation_name, branch_name, instructor_name, location_name, description | uuid, course_uuid, program_uuid, organisation_uuid, branch_uuid, default_instructor_uuid, category_uuid, is_active, class_visibility, content_approved, location_type, session_format, starts_at, registration_closes_at, sale_price, created_at | starts_at, sale_price, created_at, title | active `PUBLIC` OR staffed org OR taught OR enrolled | `ClassDefinition`, `ClassSessionTemplate`; `UserUpdateEvent` re-indexes the classes of a renamed instructor | course/program names and approval, organisation and branch names until the nightly rebuild (module reads re-check approval and visibility on the rows) |
| `marketplace_jobs` | title, course_name, program_title, organisation_name, branch_name, location_name, target_groups, description | status, organisation_uuid, branch_uuid, course_uuid, program_uuid, category_uuid, location_type, session_format, starts_at, registration_closes_at, uuid, created_at | created_at, starts_at | `OPEN`; staff of the filtered organisation see it in any status | `ClassMarketplaceJob`, `ClassMarketplaceJobSessionTemplate`; bulk deletes enqueue explicitly | course/program, organisation and branch names until the nightly rebuild |
| `instructors` | full_name, professional_headline, skills, experience_positions, experience_organisations, location_name, bio | admin_verified, active, skills, skill_levels, location_name, uuid, created_at | full_name, rating_avg, review_count, created_at | verified OR own profile OR pinned by `uuid` | `Instructor`, `InstructorSkill`, `InstructorExperience`, `InstructorReview`; `UserUpdateEvent` (name kept by a DB trigger) | none |
| `organisations` | name, slug, location, description | active, admin_verified, country, uuid, created_at | name, created_at | active AND verified | `Organisation` | none |
| `people` | full_name, first_name, last_name, email, username, user_no | domains, organisation_uuids, branch_uuids, active, is_platform_admin, is_org_admin, uuid, created_at | full_name, created_at | admins only; roster: members of a managed organisation, names only | `User`, `UserDomainMapping`, `UserOrganisationDomainMapping` | none |

- **Staleness window.** A name copied from another module (organisation, branch, course, program,
  course-creator) is refreshed when the copying document itself changes, and otherwise by the nightly
  full rebuild, so it can lag a rename by **up to about 24 hours**. Instructor renames on classes
  follow within seconds (via `UserUpdateEvent`). Visibility never goes stale on the module read paths:
  hydration re-checks every hit against the database, and when rows drop out the page's total is
  restated from what is shown (`SearchResults.total`). Global search does not hydrate, so a visibility
  change there lags by the indexing delay (seconds).
- **Hydration** (module read paths) loads the hit UUIDs in one query that re-applies the caller's SQL
  visibility, so a lagging document can drop out but never leak; instructor pay is redacted exactly as
  on the database path.
- **Instructor visibility** is one rule for both paths (`InstructorVisibility`): platform admins see
  every profile; everyone else discovers verified instructors plus their own profile; an exact identity
  lookup (`uuid`/`uuid_in`, `user_uuid`/`user_uuid_in`) resolves the named profiles whatever their
  state, as `GET /instructors/{uuid}` does - which keeps the organisation, course-creator and booking
  screens able to name an instructor who is not verified yet.
- **Fallback** (reads off or engine down): a case-insensitive, escaped match (`name_like`,
  `title_like`, `fullName_like` or the class title) inside the same scoped SQL query. On
  `GET /api/v1/classes?q=` the other filter parameters are applied to the database too
  (`ClassSearchFallbackFilter`), so both paths answer the same question.
- **Unpaged class lists** (`/classes/active`, `/classes/organisation/{uuid}`) return at most 100 matches
  for `q`; paged endpoints cap a page at 100.

## Go-live runbook

1. **Create the scoped key** once, with the master key, and store it as `SEARCH_MEILISEARCH_API_KEY`:

   ```bash
   curl -X POST http://meilisearch:7700/keys -H "Authorization: Bearer $MEILI_MASTER_KEY" \
     -H 'Content-Type: application/json' \
     -d '{"description":"elimika api","actions":["search","documents.*","indexes.*","settings.*","tasks.get","stats.get"],"indexes":["*"],"expiresAt":null}'
   ```

2. **Turn indexing on**: set `SEARCH_ENABLED=true` (leave every `SEARCH_READENABLED_*` unset) and
   redeploy. Writes now reach the indexes; every read still comes from PostgreSQL, and global search
   answers 503 because no index is read-enabled.
3. **Build each index**: `POST /api/v1/admin/search/rebuild` (all), or
   `POST /api/v1/admin/search/indexes/{index}/rebuild` one at a time. Rebuilds run in the background.
4. **Verify counts**: `GET /api/v1/admin/search/indexes` until every index shows `status: READY`,
   `engine_document_count` equal to `recorded_document_count`, no `last_error`, and (after the next hourly reconcile)
   `drift` 0.
5. **Enable reads one index at a time**: set `SEARCH_READENABLED_<INDEX>=true` for one index, redeploy,
   and check its `q` listings and `GET /api/v1/search?types=<index>` before the next. Suggested order:
   `organisations`, `courses`, `programs`, `classes`, `marketplace_jobs`, `instructors`, `rubrics`,
   `people` last.
6. **Roll back** by turning the flag off: `SEARCH_READENABLED_<INDEX>=false` sends that index's reads
   back to PostgreSQL and drops the type from global search on the next deploy; nothing needs
   rebuilding. `SEARCH_ENABLED=false` switches everything off (no indexing, no HTTP, global search 503).

## Adding a searchable entity

Everything below lives in the **owning module** (e.g. `course/internal/search/`). Nothing is added to the
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

### 6. Global search

Add a `GlobalSearchProvider` bean next to the scope factory, so the type shows up in
`GET /api/v1/search`:

```java
@Component
class CourseGlobalSearchProvider implements GlobalSearchProvider {
    public String type() { return "courses"; }                        // the API's type name
    public SearchIndexDefinition definition() { return CourseSearchSource.DEFINITION; }

    // The same rule as the listing; empty hides the type from this caller. Fail closed for anonymous callers.
    public Optional<SearchScope> scopeForCurrentCaller() { ... }

    // Optional: restrict what the text may match for this caller (names, never emails).
    public List<String> searchOnForCurrentCaller() { return null; }

    // From stored fields only - no database query. Only fields the index already holds can appear.
    public GlobalSearchHit toHit(SearchHit hit) {
        return new GlobalSearchHit(type(), hit.uuid(), GlobalSearchHit.text(hit.document(), "name"),
                GlobalSearchHit.text(hit.document(), "creator_name"), null, GlobalSearchHit.highlight(hit, "name"));
    }
}
```

The search module discovers providers by type and adds nothing of its own; the type goes live with
the index's `SEARCH_READENABLED_<INDEX>` flag.
