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
  `SearchUnavailableException`.
- **`q` requires search. There is no database fallback.** Free text is served only by Meilisearch.
  A request with `q` whose index cannot answer (search off, the index's read flag off, or the engine
  failing) is a **503** `{"success": false, "message": "Search is unavailable"}`. PostgreSQL serves only
  relational filters (`eq, in, notin, noteq, gt, gte, lt, lte, between, notingroup`) through
  `GenericSpecificationBuilder`; the SQL text operators `_like`, `_startswith` and `_endswith` were
  removed and answer **400** ("Text operators were removed; use the q parameter for text search").

## Flow

```
 UI                        API (owning module)                    storage
 ──                        ───────────────────                    ───────
 GET /courses?q=java&...
        │
        ▼
 Controller ──► Service ── q present?
                   │            │ no ──► relational filters only: JPA Specification ──► PostgreSQL
                   │ yes
                   ├── SearchAvailability.isReadEnabled("courses")?
                   │            │ no ──► SearchUnavailableException ──► 503 "Search is unavailable"
                   │            │        (GlobalExceptionHandler; never a SQL LIKE)
                   │ yes
                   ├── every other param/sort in the index allow-list (after aliases)?
                   │            │ no ──► IllegalArgumentException naming the key ──► 400
                   │ yes
                   ▼
            SearchRequest(index, text, SearchParamsTranslator.toFilter(params),
                          scope = CourseSearchScopes.forCaller(...))
                   │
                   ▼
            SearchGateway.search ─────────────────────────────────► Meilisearch
            (scope AND filter, quoted and escaped)                   (index "courses")
                   │ engine error ──► SearchUnavailableException ──► 503
                   ▼
            SearchPage { hits[uuid, document, formatted], total_hits, facets }
                   │ hydrate hit UUIDs in one SQL query that re-applies visibility
                   ▼
            the endpoint's existing DTO page, in hit order


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
            provider.toHit(hit) → GlobalSearchHit { type, uuid, title, subtitle, image_url, highlight,
                                                    context? { course_uuid, lesson_uuid? } }
                                │ built from stored document fields, no PostgreSQL query
                                ▼
 ApiResponse { hits: [...grouped by type, in request order], totals: { courses: 12, people: 3 } }
```

## Configuration

| Property | Env var | Default | Meaning |
|---|---|---|---|
| `search.enabled` | `SEARCH_ENABLED` | `false` | Master switch for indexing and search |
| `search.read-enabled.<index>` | `SEARCH_READENABLED_<INDEX>` | `false` | Serve reads for this index from search - both the module's `q` listings and global search. Indexing runs whenever search is enabled; reads are opted in separately. **While it is off, every `q` on that index's endpoints answers 503** |
| `search.auto-rebuild` | `SEARCH_AUTO_REBUILD` | `false` | Rebuild on startup when a definition's `schemaVersion` moved on |
| `search.full-rebuild-cron` | `SEARCH_FULL_REBUILD_CRON` | `0 30 1 * * *` | Nightly blue/green rebuild of every index, UTC. `-` disables it |
| `search.meilisearch.host` | `SEARCH_MEILISEARCH_HOST` | `http://meilisearch:7700` | Engine URL (internal Docker network only) |
| `search.meilisearch.api-key` | `SEARCH_MEILISEARCH_API_KEY` | empty | A **scoped** key, never the master key |
| `search.meilisearch.connect-timeout` / `read-timeout` | `SEARCH_MEILISEARCH_CONNECT_TIMEOUT` / `_READ_TIMEOUT` | `PT2S` / `PT5S` | HTTP timeouts for searches and document writes |
| `search.meilisearch.admin-read-timeout` | `SEARCH_MEILISEARCH_ADMIN_READ_TIMEOUT` | `PT30S` | Read timeout for index administration (create, settings, swap, delete), which the engine can answer slowly while it rebuilds. Never shorter than `read-timeout` |
| `search.startup-retry-attempts` / `startup-retry-initial-backoff` | `SEARCH_STARTUP_RETRY_ATTEMPTS` / `_INITIAL_BACKOFF` | `5` / `PT1S` | Startup ensure-index and settings retries per index; the pause doubles after each failure (1 s, 2 s, 4 s, 8 s) |
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
| `course_content` | `SEARCH_READENABLED_COURSE_CONTENT` (not enabled yet; staging first) |

`docker/compose.yaml` runs `getmeili/meilisearch` (pinned tag, no published port, 512 MB limit)
with `MEILI_MASTER_KEY` from `docker/meilisearch.env` (kept out of `.env` so the API never sees it; the staging deploy generates it once). The application key is created once with the master key
(see the runbook below).

## Global search endpoints

Registered whether or not search is on, so a disabled engine answers **503** rather than 404.
`GET` on both routes is `permitAll` in `SecurityConfiguration`: each type's provider decides what the
caller sees, and `DomainSecurityService` answers "no user, no roles" for an anonymous request, so every
provider falls back to its public boundary or hides the type.

Both endpoints carry explicit OpenAPI `operationId`s - `globalSearch` and `searchByType` - so the
generated client keeps stable method names.

### `GET /api/v1/search?q=&types=&limit=`

| Param | Rule |
|---|---|
| `q` | required, 2-200 characters after trimming, else 400 |
| `types` | comma-separated; omitted means all nine in the order `courses, programs, classes, marketplace_jobs, instructors, organisations, people, rubrics, course_content`. An unknown type is a 400; a type the caller may not see, or whose index is not read-enabled, is skipped silently |
| `limit` | hits per type, 1-20, default 5, else 400 |

```json
{
  "success": true,
  "data": {
    "hits": [
      { "type": "courses", "uuid": "…", "title": "Astronomy 101", "subtitle": "Ada Creator",
        "image_url": "/api/v1/files/…", "highlight": "<em>Astro</em>nomy 101" },
      { "type": "people", "uuid": "…", "title": "Asha Otieno", "subtitle": "student", "image_url": null,
        "highlight": "<em>Asha</em> Otieno" },
      { "type": "course_content", "uuid": "…", "title": "Stellar spectra", "subtitle": "Lesson 3 · Astronomy 101",
        "image_url": null, "highlight": "<em>Astro</em>physics of …",
        "context": { "course_uuid": "…", "lesson_uuid": "…" } }
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
- **People are re-checked in SQL.** A provider may override `recheck(hits)`; `people` does, running the
  roster membership predicate over the page in one query, so a revoked membership never surfaces even
  while the index lags. Dropped hits restate that type's total.
- **No hydration otherwise.** `GlobalSearchHit` is built from stored document fields by the owning module's
  provider, so a change reaches global search within seconds (the async indexing delay), and a stale
  denormalised name stays until the nightly rebuild (see the staleness column below).
- **`context`** (optional, omitted when empty) tells the UI where a hit lives so it can deep-link it:
  `course_content` hits carry `{course_uuid, lesson_uuid}` (a lesson hit's `lesson_uuid` is its own
  uuid), `classes` hits carry `{course_uuid}` when the class has a course. Built from displayed
  document attributes, never a database query. The same object appears on `GET /api/v1/search/{type}`.
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
- `near=lat,lng&radius_km=` is a near-me search for `instructors`, `classes` and `marketplace_jobs` only
  (any other type is a 400), signed-in callers only (403). Hits carry `distance_band`; see
  [Near-me search](#near-me-search).
- The OpenAPI description of `searchByType` ends with a **filter map**: one row per type listing its
  filterable and sortable attributes, generated from the providers' index definitions
  (`SearchTypeFilterDocumentation`), so it always matches what the endpoint accepts.
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
| `course_content` | hidden | every item of managed courses (`manageableCourseUuids`, which includes own) + published, course-scoped items of enrolled courses; class-scoped quizzes/assignments hidden from learners; guardians get nothing; **re-checked in SQL** (`recheck`); subtitle `Lesson {n} · {course}` | all |

## Public catalogue page

### `GET /api/v1/catalogue/search` (operationId `searchCatalogue`)

One ranked list of public courses **and** programmes - not tabs. Anonymous-capable (`permitAll` GET);
every caller, signed in or not, admin or not, gets the public catalogue only (`is_public` on both
indexes). Owned by the course module (`CatalogueSearchController`, `CatalogueSearchService`).

```
UI catalogue page                       API (course module)                          Storage
-----------------                       -------------------                          -------
q / show / category_uuid / level  --->  CatalogueSearchService
price / creator_uuid / sort / page        | 1. difficulty names -> uuids (SQL)  ---> course_difficulty_levels
                                          | 2. facets: ONE multi-search        ---> Meilisearch courses + programs
                                          |    (base query per index + one per          (exhaustive totals,
                                          |     group that has a selection)              facetDistribution)
                                          | 3. hits: federated multi-search    ---> Meilisearch courses + programs
                                          |    (offset/limit, merged by score           (show=all; one index for
                                          |     or by the chosen sort)                   show=courses|programmes)
                                          | 4. re-check + live counts: one SQL ---> courses+lessons+course_enrollments
                                          |    query per type per page                training_programs+program_courses
                                          |                                           +lessons+program_enrollments
                                          | 5. class counts (shared SPI)       ---> classes module: class_definitions
                                          | 6. category names (SQL)            ---> course_categories
CatalogueSearchResponse  <-----------  { content: [CatalogueItem], metadata: PageMetadata, facets }
```

| Param | Values | Notes |
|---|---|---|
| `q` | text | Optional; empty browses. Typo-tolerant. |
| `show` | `all` (default), `courses`, `programmes` | `all` is a federated search; the others search one index. |
| `category_uuid` | UUIDs, repeatable or comma list | OR within the group. Courses: `category_uuids`; programmes: `category_uuid`. |
| `level` | `beginner`, `intermediate`, `advanced`, repeatable | Mapped to `course_difficulty_levels` by name (case-insensitive; `Prep` is not a catalogue level). A course matches its `difficulty_uuid`; a programme matches when **any member course** has the level (`difficulty_uuids`) - programmes have no difficulty of their own. |
| `price` | `free`, `paid` | `is_free` on both. Both values = no filter. |
| `creator_uuid` | UUID | `course_creator_uuid` on both. |
| `sort` | `relevance`, `newest`, `rating`, `popular` | Default `relevance` with `q`, `popular` without. `newest` = `created_at:desc`; `rating` = `rating_bayes:desc, created_at:desc`; `popular` = `popularity_30d:desc, created_at:desc` (enrolments in the last 30 days: the nightly `course_learning_stats` figure for courses, computed at index time for programmes). |
| `page`, `size` | 0-based; 1-48 (default 24) | Out of range is a 400. |

**Facets (disjunctive, exact).** Every group is counted under all the other active filters with its
own selection left out. The service runs one non-federated multi-search per request holding, per
index, a base query (every filter; its exhaustive `totalHits` is that index's `show` count, and it
facets the groups with no selection) plus one query per group that has a selection (every other
filter, that group's facet). With at most three groups that is at most 8 queries in one round trip.
`show` narrows the category, level and price counts to the shown types but not the `show` counts
themselves. A programme counts under every level any of its member courses has. Selected categories
are listed even at count 0. `metadata.totalElements` is the sum of the shown indexes' exhaustive
base totals (not the federated estimate), restated from the page when a hit fails the SQL re-check.

**Card fields.** `thumbnail_url` is resolved through `FileUrlResolver.publicUrl` (as the course
catalogue snapshot does); a programme shows its first member course's thumbnail (programmes have
none of their own). `level` is the course's difficulty name, or the programme's member range
(`"Beginner → Advanced"`, one name when they agree, `null` when no member has a difficulty).
`lesson_count` (published lessons; a programme sums its members), `learner_count` (distinct learners
with an active or completed enrolment) and a programme's `course_count` are live from the SQL
re-check query; a course's `class_count` (active `PUBLIC` classes) comes from the classes module via
`ClassDefinitionLookupService#countActivePublicClassesByCourse` (one grouped query). `course_count` is
`null` for a course; `class_count` and `age_label` are `null` for a programme. `age_label` is `"<n>+"`
when the course's `age_lower_limit` is 18 or more. `highlight` is the HTML-escaped title with the
engine's `<em>` tags, `null` without `q` or when the title did not match.

**Failure.** Either index not read-enabled, search off or the engine failing: 503 "Search is
unavailable". No database fallback.

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

**Startup.** Once the application is ready, `SearchIndexStartupRunner` creates each index if needed
and applies its settings (definition plus live synonyms), then marks it stale or resumes a rebuild.
The create-and-settings step is retried with exponential backoff (`search.startup-retry-attempts`,
default 5, first pause `search.startup-retry-initial-backoff`, 1 s, doubling), because the engine can
time out (`HttpTimeoutException`) while another instance rebuilds. Failed attempts log at **warn**;
only giving up logs at **error**. Every index is prepared on its own, so one that keeps failing never
stops the others. Index administration uses its own, longer read timeout
(`search.meilisearch.admin-read-timeout`) so settings and swaps are not cut off by the 5 s search timeout.

## Indexes

| Index | Owner (source, scopes, global provider) | Source table | Module read endpoints (`q`) |
|---|---|---|---|
| `courses` | `course/internal/search`: `CourseSearchSource`, `CatalogueSearchScopes.courses`, `CatalogueGlobalSearchProviders.Courses` | `courses` (root courses only) | `GET /api/v1/courses`, `/courses/search` and the catalogue listings |
| `programs` | `course/internal/search`: `ProgramSearchSource`, `CatalogueSearchScopes.programs`, `…Programs` | `training_programs` | `GET /api/v1/programs`, `/programs/search`, the admin approval queue `GET /api/v1/admin/programs/pending` (admin scope; hits re-checked against the queue filter) |
| `rubrics` | `course/internal/search`: `RubricSearchSource`, `CatalogueSearchScopes.rubrics` / `publicRubrics`, `…Rubrics` | `assessment_rubrics` | `GET /api/v1/rubrics/search`, `/rubrics/discovery/search` |
| `classes` | `classes/search`: `ClassSearchSource`, `ClassSearchScopes`, `ClassesGlobalSearchProviders.Classes` | `class_definitions` | `GET /api/v1/classes`, `/classes/active`, `/classes/organisation/{uuid}` |
| `marketplace_jobs` | `classes/search`: `MarketplaceJobSearchSource`, `MarketplaceJobSearchScopes`, `…MarketplaceJobs` | `class_marketplace_jobs` | `GET /api/v1/classes/jobs` |
| `instructors` | `instructor/search`: `InstructorSearchSource`, `InstructorSearchScopes` + `InstructorVisibility`, `InstructorGlobalSearchProvider` | `instructors` | `GET /api/v1/instructors`, `/instructors/search` (every row, with or without `q`, carries `rating_avg` and `review_count` from one aggregate query per page) |
| `organisations` | `tenancy/search`: `OrganisationSearchSource`, `OrganisationSearchScopes`, `TenancyGlobalSearchProviders.Organisations` | `organisation` | `GET /api/v1/organisations`, the pending-verification queue |
| `course_content` | `course/internal/search`: `CourseContentSearchSource`, `CourseContentEntitlement`, `CourseContentGlobalSearchProvider` | `lessons`, `lesson_contents`, `quizzes`, `assignments` (root courses only) | `GET /api/v1/courses/{courseUuid}/content/search` (see below) |
| `people` | `tenancy/search`: `PeopleSearchSource`, `PeopleSearchScopes`, `TenancyGlobalSearchProviders.People` | `users` | `/users/search`, `/admin/users/eligible`, organisation rosters, and the instructor-student roster `search` (timetabling, see below) |

### Fields, scopes, triggers and staleness

| Index | Searchable (priority order) | Filterable | Sortable | Scope (non-admin) | Re-indexed by | Known staleness |
|---|---|---|---|---|---|---|
| `courses` (v3) | name, category_names, creator_name, difficulty_name, description, objectives | status, active, admin_approved, is_public, course_creator_uuid, category_uuids, difficulty_uuid, is_free, price, uuid, created_at, level_order, prerequisite_uuids, age_lower_limit, age_upper_limit, skill_uuids | name, created_at, price, rating_avg, enrolment_count, completion_rate, popularity_30d, rating_bayes | `is_public` OR own creator OR related (enrolled / manageable) course | `Course`, `CourseCategoryMapping`, `CourseReview`, `CourseEnrollment`, `CoursePrerequisite`, `CourseSkill`; fan-out on `Category`, `DifficultyLevel` | `creator_name` (course-creator module); `completion_rate`, `popularity_30d`, `rating_bayes` from `course_learning_stats` (01:00 UTC job) until the nightly rebuild |
| `programs` (v2) | title, course_names, category_name, creator_name, description | status, is_published, admin_approved, active, is_public, course_creator_uuid, category_uuid, is_free, uuid, created_at, difficulty_uuids | title, created_at, rating_avg, rating_bayes, popularity_30d, enrolment_count (ranking rules identical to `courses`, so the two merge in one federated ranking) | `is_public` OR authored under own identities | `TrainingProgram`, `ProgramCourse`, `ProgramReview`, `ProgramEnrollment`; fan-out on `Course`, `Category`, `DifficultyLevel` | `creator_name`, member-course thumbnail and level range until the nightly rebuild |
| `rubrics` | title, rubric_type, description | is_public, is_active, status, course_creator_uuid, rubric_type (stored lower-case, schema v2), usage_count, uuid, created_at | title, created_at, usage_count | public OR own; discovery: public AND active | `AssessmentRubric`, `CourseRubricAssociation` | none |
| `classes` (v2) | title, course_name, program_title, organisation_name, branch_name, instructor_name, location_name, description | uuid, course_uuid, program_uuid, organisation_uuid, branch_uuid, default_instructor_uuid, category_uuid, is_active, class_visibility, content_approved, location_type, session_format, starts_at, registration_closes_at, sale_price, created_at, `_geo` | starts_at, sale_price, created_at, title, `_geo` | active `PUBLIC` OR staffed org OR taught OR enrolled | `ClassDefinition`, `ClassSessionTemplate`; `UserUpdateEvent` re-indexes the classes of a renamed instructor | course/program names and approval, organisation and branch names until the nightly rebuild (module reads re-check approval and visibility on the rows) |
| `marketplace_jobs` (v3) | title, course_name, program_title, required_skill_names, organisation_name, branch_name, location_name, target_groups, description | status, organisation_uuid, branch_uuid, course_uuid, program_uuid, category_uuid, location_type, session_format, starts_at, registration_closes_at, uuid, created_at, required_skill_uuids, `_geo` | created_at, starts_at, `_geo` | `OPEN`; staff of the filtered organisation see it in any status | `ClassMarketplaceJob`, `ClassMarketplaceJobSessionTemplate`, `ClassMarketplaceJobRequiredSkill`; `CourseSkillsChangedEvent` re-indexes the course's jobs; bulk deletes enqueue explicitly | course/program, organisation, branch and skill names until the nightly rebuild |
| `instructors` (v3) | full_name, professional_headline, skills, experience_positions, experience_organisations, location_name, bio | admin_verified, active, skills, skill_levels, skill_uuids, location_name, uuid, created_at, `_geo` | full_name, rating_avg, review_count, created_at, `_geo` | verified OR own profile OR pinned by `uuid` | `Instructor`, `InstructorSkill`, `InstructorExperience`, `InstructorReview`; `UserUpdateEvent` (name kept by a DB trigger) | none |
| `organisations` | name, slug, location, description | active, admin_verified, country, uuid, created_at | name, created_at | active AND verified | `Organisation` | none |
| `course_content` | title, body | type, course_uuid, lesson_uuid, published, scope, class_definition_uuid, content_type, uuid | lesson_number, display_order, updated_at | `course_uuid` IN managed OR (`course_uuid` IN enrolled AND `published` AND `scope` = COURSE) | `Lesson`, `LessonContent`, `Quiz`, `Assignment`; fan-out on `Lesson` (its children) and `Course` (every item, and the live course of a shadow) | lesson title/number and course name follow within seconds; content type names until the nightly rebuild; hard-deleted lessons' children until the nightly rebuild (the SQL re-check hides them meanwhile) |
| `people` (schema v2) | full_name, first_name, last_name, email, username, user_no | domains, organisation_uuids, branch_uuids, active, is_platform_admin, is_org_admin, uuid, created_at, email_normalized | full_name, created_at | admins only (every attribute); roster: members of a managed organisation by name and email (`searchOn` adds `email`; a `q` containing `@` is an exact `email_normalized` filter); username and user_no stay admin-only; global search stays names only | `User`, `UserDomainMapping`, `UserOrganisationDomainMapping` | none |

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
- **Unpaged class lists** (`/classes/active`, `/classes/organisation/{uuid}`) return at most 100 matches
  for `q`; paged endpoints cap a page at 100.

### Skill tags in the indexes

The skills taxonomy (module `skills`, see [skills-taxonomy.md](skills-taxonomy.md)) reaches three indexes, each
through the module that owns the tag table:

| Index | Attribute | Filled from | Notes |
|---|---|---|---|
| `instructors` v2 | `skill_uuids` (filterable) | `instructor_skills.skill_uuid` | Only skills whose free-text name resolved to a taxonomy entry; the names stay in `skills` |
| `courses` v2 | `skill_uuids` (filterable) | `course_skills` | Heaviest first |
| `marketplace_jobs` v2 | `required_skill_uuids` (filterable), `required_skill_names` (searchable) | the job's own `class_marketplace_job_required_skills`, or its course's `course_skills` when it has none | So `q=kubernetes` finds a job tagged "Kubernetes", or whose course is |

All three moved their `schemaVersion` to 2: rebuild them (`POST /api/v1/admin/search/rebuild`) or enable
`SEARCH_AUTO_REBUILD` after deploying. A renamed or deleted skill reaches documents at the nightly rebuild
(deleting a skill cascades in SQL, which fires no trigger).

### Skill names and aliases as synonyms

The same three indexes take the taxonomy's words as **engine synonyms**: every active skill's name and
each of its aliases, lower-cased, map to each other. "JavaScript" with alias "JS" gives
`javascript → [js]` and `js → [javascript]`, so `q=js` finds a job tagged JavaScript. Retired skills are
left out.

```
 Admin UI ── POST/PUT/DELETE /api/v1/admin/skills ──► SkillService (skills) ──► skills table
                                                         │ publishes SearchSynonymsChanged("skills")
                                                         │ (shared.search; after commit)
                                                         ▼
                                    SearchSynonymsRefresher (search, single index thread)
                                         │ every definition with synonymSources ∋ "skills":
                                         │   courses, marketplace_jobs, instructors
                                         ▼
                                    SearchDefinitionResolver.effective(definition)
                                         │ definition.synonyms ∪ SearchSynonymSource("skills").synonyms()
                                         │                       (SkillSearchSynonyms, reads skills)
                                         ▼
                                    SearchIndexAdmin.ensureIndex ── PATCH /indexes/{uid}/settings ──► Meilisearch
                                    (also the build index while a rebuild runs; no reindex, no schema bump)
```

- **Engine-neutral.** An index opts in with `SearchIndexDefinition.withSynonymSources(List.of(SearchSynonymSource.SKILLS))`.
  The search module merges the live words in wherever it applies settings: startup, blue/green rebuilds and
  the refresh above. The skills module only implements `shared.search.SearchSynonymSource` and publishes
  `shared.search.SearchSynonymsChanged`; neither module depends on the other.
- **Durable.** The refresh is a Modulith `@TransactionalEventListener`: a failed settings call leaves the
  publication incomplete and it is resubmitted like any indexing request.
- **No rebuild.** Synonyms apply at query time, so changing them needs neither a reindex nor a schema bump.

## `q` rules: no SQL fallback

Free text has exactly one path - the index. Every module endpoint that accepts `q` follows the same
rules; nothing falls back to PostgreSQL.

| Situation | Answer |
|---|---|
| `q` absent or blank | Relational SQL listing, as before (`GenericSpecificationBuilder` + the module's own relational keys) |
| `q` present, index read-enabled, engine healthy | Search, hydrated in hit order |
| `SEARCH_ENABLED=false`, or `SEARCH_READENABLED_<INDEX>` off | **503** `{"success": false, "message": "Search is unavailable", "error": "Text search (q) is disabled or temporarily unavailable; try again later"}` |
| The engine errors or times out | **503**, same body |
| `q` with a filter or sort the index cannot express (e.g. `category_name`, `sort=lastModifiedDate`, any `_like`) | **400** naming the key |
| `q` with a page size above 100, or unpaged | **400** (catalogue endpoints) |
| Any `_like`, `_startswith`, `_endswith` key, with or without `q` | **400** "Text operators were removed; use the q parameter for text search" |

`SearchUnavailableException` is mapped once, in `GlobalExceptionHandler`, and the UI detects an outage by
the exact message `Search is unavailable`.

**Aliases** (cheap and unambiguous; anything else is a 400):

| Endpoint | Request key with `q` | Index filter |
|---|---|---|
| courses | `is_published` / `is_draft` / `is_archived` / `is_in_review` = `true` / `false` | `status = value` / `status != value` |
| courses, programs | `lifecycle_stage` (also `lifecycle_stage_in`, …) | `status` |
| courses, programs, rubrics | `status`, `status_eq` in any case | `status` (stored lower-case) |
| rubrics | `rubric_type` in any case | `rubric_type` (stored lower-case) |
| classes | `location_type`, `session_format`, `class_visibility` in any case | the enum constant name |
| `/users/search` | `user_domain` | `domains` |
| sorts | `createdDate`, `created_date` | `created_at` |

`lastModifiedDate` is **not** aliased: modification time is not in the documents and `created_at` means
something else, so `sort=lastModifiedDate` with `q` is a 400. Sort by relevance (no `sort`) instead.

**Removed SQL text paths** (all now answered by the index or gone):

- `GenericSpecificationBuilder` `like` / `startswith` / `endswith` and `LikePatterns`.
- `UserSpecificationBuilder` `full_name`, `full_name_like` and the name-or-email helper.
- `CourseSpecificationBuilder` `category_name` and `difficulty_name` (use `category_uuids` /
  `difficulty_uuid`, or `q`, which searches category and difficulty names).
- `AssessmentRubricRepository` title/description `LIKE` queries and the `RubricTypeContaining` lookup.
  Rubric discovery `type` is now an **exact, case-insensitive** match: through the index with `q`,
  `LOWER(rubric_type) = :type` in SQL without.
- The admin-eligible users term search, the organisation and pending-queue name matches, the class
  title match and `ClassSearchFallbackFilter`, the marketplace job `searchByTitle`, and the instructor
  `fullName_like` fallback.
- The timetabling instructor-student roster's `full_name ILIKE`.
- `InstructorEducationRepository.findByQualificationContainingIgnoreCase` (unused).

**Categories are not indexed.** `GET /config/categories/search` keeps its relational filters, but there
is no name search any more (`name_like` is a 400); the UI lists `GET /config/categories` and filters the
small set client-side.

### In-course content search (`course_content`)

`GET /api/v1/courses/{courseUuid}/content/search?q=&types=lesson,content,quiz,assignment&page=&size=`

```
 UI (course page search box)
   │ q=photosynthsis&types=lesson,quiz
   ▼
 CourseContentSearchController
   │ @PreAuthorize: platform admin OR canReadCourseAsLearner OR canManageCourseGradebook ── no ──► 403
   ▼
 CourseContentSearchService
   │ q blank / > 200 chars, unknown type, page < 0, size outside 1-100 ──► 400
   │ SEARCH_READENABLED_COURSE_CONTENT off, search off, engine error ──► 503 "Search is unavailable"
   │ scope = CourseContentEntitlement (managed ∪ enrolled-published-course-scoped)
   │ filter = course_uuid = {courseUuid} AND type IN types
   ▼
 SearchGateway ─────────────────────────────► Meilisearch index "course_content"
   │ hits (stored fields only)
   ▼
 CourseContentEntitlement.retainVisible ────► PostgreSQL: one UNION query over lessons, lesson_contents,
   │ (engine narrows, SQL authorizes)          quizzes, assignments joined to root courses
   ▼
 ApiResponse<PagedDTO<{ type, uuid, lesson_uuid, lesson_number, lesson_title, title, highlight }>>
```

- **One document per item**, keyed by the item UUID: `uuid`, `type` (`lesson`/`content`/`quiz`/`assignment`),
  `course_uuid`, `lesson_uuid`, `lesson_number`, `lesson_title`, `course_name`, `title`, `body`
  (description + learning objectives / content text / instructions, markup stripped, truncated to 2000),
  `content_type`, `scope` (`COURSE`/`CLASS`), `class_definition_uuid`, `published`, `display_order`, `updated_at`.
- **`published`** is what an enrolled learner may see: the lesson published and active, and for a quiz or
  assignment the item itself published too (the `LearnerMaterialScope` rule).
- **Never indexed:** quiz questions and options (answers), rubric data, submissions, `file_url`.
  Shadow-draft courses (`parent_course_uuid` set) are excluded.
- **Staff on a learner's dashboard** (acting-domain header) lose the managed branch, as on the material
  endpoints (`CourseFootingCap`). Guardians are excluded in v1.
- `highlight` is an excerpt of about 200 characters around the first match in `title` or `body`.

### Instructor-student roster search (timetabling)

`GET /api/v1/organisations/{org}/instructors/{instructor}/students?search=` has no index of its own:

```
 UI ── search=amina ──► InstructorStudentRosterServiceImpl
                            │ people reads enabled?  no ──► 503
                            ▼
        EnrollmentRepository.findAllInstructorStudentsForOrganisation  (SQL, no name filter)
        organisation + instructor of record (+ class) - the roster is already authorised in SQL
                            │ roster rows + each student's user_uuid
                            ▼
        SearchGateway.search("people", text,
            scope = uuid IN [roster user uuids],          ◄── can only narrow the roster
            searchOn = full_name, first_name, last_name)  ◄── never email / username
                            │ ranked user uuids (pages of 100 until every match is read)
                            ▼
        roster rows of the matched users, in rank order, paged in memory ──► InstructorStudentPageDTO
```

Timetabling reaches the index through `shared.search.SearchGateway` (the `shared` module it already
depends on) and names the index by its string; it imports nothing from tenancy's search package.

### Organisation roster search by email

`GET /api/v1/organisations/{uuid}/users?q=` lets a manager of that organisation match members by name
**and email** - the roster DTO already shows them their members' emails, so the search reveals nothing
new. Username and user number stay admin-only, and global people search stays names only.

```
 UI ── q=amina / q=Amina.W@School.ke ──► UserService.getUsersByOrganisation
                            │ PeopleSearchService.searchOrganisationRoster
                            │ people reads enabled?  no ──► 503
                            ▼
        scope = organisation_uuids = {uuid}                  (manages {uuid}? no ──► 403)
        q has "@"?  no  ──► text = q,    searchOn = full_name, first_name, last_name, email
                    yes ──► text = none, filter email_normalized = lower(trim(q))  ◄── exact address only
                            │ ranked user uuids
                            ▼
        UserRepository.findOrganisationMembersByUuidIn  (SQL re-check: active, non-deleted membership)
                            ▼
        Page<UserDTO>  ◄── users.email ── people document { email, email_normalized }
```

A local-part prefix (`q=amina.w`) matches through normal search; once the query contains `@` only the
exact address matches, so a partial `amina@sch` finds nothing.

## Geo, ranking score and matching strategy

The engine-neutral contract carries location and relevance without exposing Meilisearch types:

| Contract | Rendered as | Needs |
|---|---|---|
| `SearchFilter.geoRadius(lat, lng, meters)` | `_geoRadius(lat, lng, meters)` | `_geo` in `filterableAttributes` |
| `SearchSort.geoPoint(lat, lng)` (nearest first) | `_geoPoint(lat,lng):asc` | `_geo` in `sortableAttributes` |
| `SearchRequest.withMatchingStrategy(LAST \| ALL \| FREQUENCY)` | `matchingStrategy` | nothing; `null` keeps the engine default (`last`) |
| `SearchHit.rankingScore()` | `showRankingScore: true` → `_rankingScore` | every paged search |
| `SearchHit.geoDistanceMeters()` | `_geoDistance` | a geo sort or radius filter |

A document carries its point as `@JsonProperty("_geo") SearchGeoPoint geo` (`{"lat":..,"lng":..}`),
or `null` to stay out of geo queries. `SearchIndexDefinition.GEO_ATTRIBUTE` names the attribute.
`_geo` is never a plain `searchParams` filter, sort or facet key: `SearchParamsTranslator` and the
facet check refuse it, so only code can build a geo query. The gateway keeps `_geo` in
`SearchHit.document()` when the engine returns it, and strips other `_`-prefixed engine fields; but
every index that stores `_geo` today leaves it out of `displayedAttributes`
(`SearchDocumentAttributes.displayedWithoutGeo`), so it never comes back. Meilisearch then also omits
`_geoDistance`, which is why near-me bands are worked out in the application (below). With search disabled, every call still throws
`SearchUnavailableException`.

```
request (lat,lng,radius)  ─►  SearchFilter.geoRadius + SearchSort.geoPoint
                                   │ MeilisearchFilterRenderer
                                   ▼
        POST /indexes/{uid}/search {filter:"… AND (_geoRadius(…))", sort:["_geoPoint(…):asc"], showRankingScore:true}
                                   │
                                   ▼
        SearchHit(uuid, document incl. _geo, formatted, rankingScore, geoDistanceMeters)
```

## Near-me search

`near=lat,lng&radius_km=` finds instructors, classes and marketplace jobs around a point, with or
without `q`, on `GET /api/v1/instructors`, `GET /api/v1/classes`, `GET /api/v1/classes/jobs` and
`GET /api/v1/search/{instructors|classes|marketplace_jobs}`.

```
UI (browser geolocation or a picked place)
  │  GET /api/v1/instructors?near=-1.2921,36.8219&radius_km=5[&q=…]
  ▼
Controller ── NearMe.parse: near rounded to 2 dp (-1.29,36.82), radius clamped 2..100 km (default 10);
  │           400 on a malformed value (never echoed). near is not stored, not logged, and
  │           QueryStringRedactor masks near/lat/lng in request_audit_log (near=[redacted:15]).
  ▼
Module read path (InstructorSearchReader / ClassDefinitionServiceImpl / ClassMarketplaceJobServiceImpl)
  │  filter = scope AND caller filters AND _geoRadius(-1.29, 36.82, radius_km * 1000)
  │  sort   = no q: _geoPoint(...) first · with q: relevance, _geoPoint last as a tie-breaker
  ▼
Meilisearch index (`_geo` filterable + sortable, NOT displayed) ── 503 "Search is unavailable" when it
  │                                                                  cannot answer; no SQL fallback
  ▼  hit UUIDs
Hydration (SQL): visibility re-checked; row still locatable (opted-in + verified instructor,
  │  IN_PERSON/HYBRID class or job); distance band from the row's rounded point to the rounded near
  ▼
Response rows: distance_band ("<2 km" | "2-5 km" | "5-10 km" | "10-25 km" | ">25 km"),
               coordinates rounded to 2 dp; never metres, never the searcher's point
```

**What gets a `_geo` point** (always through `SearchGeoPoint.rounded`, i.e.
`CoordinatePrecision.toPublic`, about 1 km):

| Index | Point | Only when |
|---|---|---|
| `instructors` | the profile's `lat`/`long` | `location_search_opt_in` AND `admin_verified` AND both coordinates |
| `classes` | the class's coordinates, else its branch's pin (`TrainingBranchLookupService`) | `location_type` IN_PERSON or HYBRID |
| `marketplace_jobs` | the job's coordinates, else its branch's pin | `location_type` IN_PERSON or HYBRID |
| `people`, and every other index | never | - |

Minors are never locatable: only instructors (adults, and only by their own opt-in), venues, classes and
jobs carry points.

**Instructor opt-in.** `instructors.location_search_opt_in` (`BOOLEAN NOT NULL DEFAULT FALSE`) is set
by the owner only - not even a platform admin - through
`PUT /api/v1/instructors/{uuid}/location-search` with `{"enabled": true|false}`. The entity trigger
re-indexes the profile, adding or dropping its point. The flag appears as `location_search_opt_in` on
the owner's own profile (single read, list rows and near-me rows) and nowhere else.

**Rules.**

- Instructors keep their usual scope (verified + own profile; admins everything); an unverified or
  opted-out instructor has no point, so never matches. Near-me on instructors, and on any type through
  global search, needs a signed-in caller (403 otherwise).
- `near` alone is enough; `radius_km` without `near` is a 400.
- **Distance bands are computed in the application.** Meilisearch reports `_geoDistance` only when
  `_geo` is displayed, and it never is. The module read paths compute the band from the hydrated
  row's rounded point; global search asks the owning module through
  `GlobalSearchProvider.nearMePoints(uuids)` (one query per page) and drops a hit that is no longer
  locatable. Both ends are the same rounded points the engine filtered on (great-circle distance), so
  the band matches the engine's filter.
- Global search logs neither `near` nor the engine's error text for a near-me request.

## Discovery tracking

Recommenders record what they showed; the client reports what the user did with it. Job matching
(see [job-matching.md](job-matching.md)) records on the `job_matches` and `job_candidates` surfaces with
model version `rules-v1`. Both land in
`discovery_events` (180-day retention, purged nightly at 02:45 UTC by `DiscoveryEventPurgeJob`).
No query text is ever stored: surface and item type are `[a-z0-9_]` slugs, reason codes
`[A-Za-z0-9_]` codes.

```
recommender ── DiscoveryTracker.recordImpressions(user, surface, recommendation_id, model_version, items)
                    │ own transaction; errors logged, never thrown
                    ▼
              discovery_events (IMPRESSION, position, reason_codes[], model_version, created_at UTC)
                    ▲
UI ── POST /api/v1/discovery/events {recommendation_id, item_uuid, item_type, event_type, position}
      authenticated; user = principal; event_type CLICK | DISMISS (IMPRESSION → 400); always 202.
      Stored only when it matches an impression shown to that user, inheriting its surface,
      reason codes and model version; anything else is dropped silently.
```

Conversions (enrol, apply, hire) are never reported by the client; they are joined from the source
tables afterwards.

## Go-live runbook

1. **Create the scoped key** once, with the master key, and store it as `SEARCH_MEILISEARCH_API_KEY`:

   ```bash
   curl -X POST http://meilisearch:7700/keys -H "Authorization: Bearer $MEILI_MASTER_KEY" \
     -H 'Content-Type: application/json' \
     -d '{"description":"elimika api","actions":["search","documents.*","indexes.*","settings.*","tasks.get","stats.get"],"indexes":["*"],"expiresAt":null}'
   ```

> **Order matters since the SQL fallback was removed.** `q` is served only by search. Search must be
> enabled, every index built, and **every `SEARCH_READENABLED_*` flag on BEFORE the release without the
> SQL fallback deploys** - otherwise every request with `q` (course, program, rubric, class, job,
> instructor, organisation and people listings, the admin-eligible `search` and the instructor-student
> roster `search`) answers 503 "Search is unavailable". Run steps 1-5 on the previous release first.

2. **Turn indexing on**: set `SEARCH_ENABLED=true` and redeploy. Writes now reach the indexes.
   On a release without the fallback, `q` answers 503 until step 5 is done for its index.
3. **Build each index**: `POST /api/v1/admin/search/rebuild` (all), or
   `POST /api/v1/admin/search/indexes/{index}/rebuild` one at a time. Rebuilds run in the background.
4. **Verify counts**: `GET /api/v1/admin/search/indexes` until every index shows `status: READY`,
   `engine_document_count` equal to `recorded_document_count`, no `last_error`, and (after the next hourly reconcile)
   `drift` 0.
5. **Enable reads for every index**: set `SEARCH_READENABLED_<INDEX>=true` for all eight
   (`organisations`, `courses`, `programs`, `classes`, `marketplace_jobs`, `instructors`, `rubrics`,
   `people`), redeploy, and check each index's `q` listings and `GET /api/v1/search?types=<index>`.
   The rubrics index moved to schema version 2 (lower-cased `rubric_type`): rebuild it (step 3) or
   enable `SEARCH_AUTO_REBUILD` before relying on the discovery `type` filter with `q`.
   The people index moved to schema version 2 (filterable `email_normalized`): until it is rebuilt, a
   manager's roster `q` containing `@` fails because the attribute is not yet filterable.
   **`course_content`** is new and separate: rebuild it (`POST /api/v1/admin/search/indexes/course_content/rebuild`),
   check its count, then set `SEARCH_READENABLED_COURSE_CONTENT=true` (staging first). Until then the in-course
   search answers 503 and global search skips the type.
6. **Deploy the no-fallback release** only once step 5 holds on the target environment.
7. **There is no SQL rollback for text search any more.** `SEARCH_READENABLED_<INDEX>=false` turns that
   index's `q` into a 503 (and drops the type from global search); relational listings without `q`
   keep working. If the engine is down, fix or rebuild it (`POST /api/v1/admin/search/rebuild`);
   `SEARCH_ENABLED=false` switches everything off (no indexing, no HTTP, every `q` 503).

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

Keep the existing endpoint and parameters. A request with `q` goes to search and **only** to search;
never catch `SearchUnavailableException` to run a SQL `LIKE` - let it reach `GlobalExceptionHandler`
(503). Without `q`, run the relational SQL listing.

```java
if (StringUtils.hasText(q)) {
    SearchFilter filter = SearchParamsTranslator.toFilter(searchParams, DEFINITION); // unknown key -> 400
    if (!searchAvailability.isReadEnabled("courses")) {
        throw new SearchUnavailableException("Search is not enabled for courses");       // -> 503
    }
    SearchRequest request = new SearchRequest("courses", q, filter,
            CourseSearchScopes.forCaller(caller),
            SearchParamsTranslator.toSort(searchParams.get("sort"), DEFINITION),
            page, size, List.of(), null);
    return toResponse(searchGateway.search(request));   // engine error -> 503
}
return relationalQuery(searchParams, pageable);         // no q: filters only, no text
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
