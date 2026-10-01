# Local environment

A self-contained Elimika on one workstation: PostgreSQL, Meilisearch and Keycloak in Docker, the API with
the `local` Spring profile, seeded data covering every role, and a smoke suite that checks the search,
permission and recommendation rules end to end. Nothing in it talks to staging or to the shared Keycloak
(`signin.sarafrika.com`), and every credential is a throwaway local value.

## Quick start

```bash
scripts/local/up.sh        # services, backend (bootRun), seed - about 2 minutes from cold
scripts/local/smoke.sh     # the rule matrix, PASS/FAIL per check, non-zero exit on a failure
scripts/local/down.sh      # stop everything, keep the volumes
```

Requirements: Docker with Compose v2, JDK 21 (`up.sh` uses `~/.jdks/temurin-21.0.11` when `JAVA_HOME` is
unset), `python3`, `curl`, `jq`.

| Script | Does |
|---|---|
| `scripts/local/up.sh [--no-seed \| --services \| --container]` | Starts the services and waits for health, starts the API (bootRun, log in `build/local/backend.log`) or reuses one already on :38080, then seeds. `--container` runs the API from the `Dockerfile` instead (compose profile `app`) |
| `scripts/local/down.sh [--reset-keycloak \| --wipe]` | Stops the API and the containers; volumes are kept. `--reset-keycloak` drops only the Keycloak database so the realm JSON is re-imported; `--wipe` deletes every volume |
| `scripts/local/seed.sh` | Idempotent seed (safe to re-run), then the course feature refresh and a full index rebuild, waiting until every index is `READY` |
| `scripts/local/token.sh <user>` | Prints an access token (password grant), e.g. `TOKEN=$(scripts/local/token.sh qa-admin)` |
| `scripts/local/smoke.sh` | The smoke / rule-matrix suite (`smoke.py`) |

## Flow: UI ↔ API ↔ storage

```
 Browser (localhost:3000)                     Keycloak :58080  realm elimika-local
   │  NextAuth (elimika-ui, confidential, PKCE)  ──────────►  login form, users qa-*@elimika.local
   │  ◄────────── access token: resource_access.elimika-backend.roles, user_domain
   ▼
 Next.js dev server  ── /api/proxy/* + Bearer token ──►  API :38080  (bootRun, profile local → dev)
                                                         │ JwtConfig: signature against the realm JWKS
                                                         │ KeyCloakJwtAuthenticationConverter:
                                                         │   resource_access.elimika-backend.roles, "-" → ":"
                                                         │ UserSyncFilter: first request creates the users row
                                                         │   from the Keycloak admin API (dob attribute included)
                                                         │ DomainSecurityService: domains and organisation
                                                         │   memberships come from PostgreSQL, not the token
                                                         ▼
                                PostgreSQL :55432 (db elimika; db keycloak for the realm)
                                   users, user_domain_mapping, user_organisation_domain_mapping, ...
                                                         │ entity triggers → event_publication → async indexer
                                                         ▼
                                Meilisearch :57700 (9 indexes; every q goes here, no SQL fallback)

 API ── client_credentials (elimika-backend service account, realm-management roles) ──► Keycloak admin API
 scripts/local/seed.py ── password grant per QA user ──► API (same validation, events and indexing as the UI)
                       └─ psql in the postgres container ──► only what the API cannot seed (see below)
```

Roles in the token are informational: every authorisation decision reads the user's domains and
organisation memberships from the database. Creating a student, instructor or course-creator profile grants
the matching domain; creating an organisation makes its creator the organisation's admin; the platform
admin comes from the app's own bootstrap runner (`app.bootstrap.admin`, enabled in `application-local.yaml`).

## Services and ports

| Service | URL | Credentials (local only) |
|---|---|---|
| API | http://localhost:38080 (Swagger at `/swagger-ui.html`, public under `dev`) | Bearer token from `token.sh` |
| Keycloak | http://localhost:58080 (admin console) | `admin` / `admin` |
| Realm | `elimika-local`, issuer `http://localhost:58080/realms/elimika-local` | |
| Client `elimika-backend` | backend admin client (`APP_KEYCLOAK_ADMIN_CLIENTID`), service account with realm-management roles; its client roles are what the API reads | secret `elimika-local-backend-secret` |
| Client `elimika-ui` | UI client: standard flow, redirect `http://localhost:3000/*`, direct-access grants for scripts; mapper puts `resource_access.elimika-backend.roles` and `user_domain` in the access token | secret `elimika-local-ui-secret` |
| PostgreSQL | `localhost:55432`, db `elimika` | `elimika` / `elimika-local` (Keycloak: db `keycloak`, `keycloak` / `keycloak-local`) |
| Meilisearch | http://localhost:57700 | master key `elimika-local-meili-master-key` |

Volumes: `elimika-local_elimika_local_pg`, `elimika-local_elimika_local_meili`,
`elimika-local_elimika_local_storage` (container backend only).

**The master key.** On this stack only, the API uses the Meilisearch master key directly
(`SEARCH_MEILISEARCH_API_KEY` defaults to it in `application-local.yaml`). Staging and production use a
scoped key created with the master key (`search-platform.md`, go-live runbook); never copy this there.

## Variables

`scripts/local/*` export `docker/.env.local`, created from the committed `docker/.env.local.example` on first
use (gitignored; values already in the environment win). `application-local.yaml` holds the same values as
defaults, so a plain `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` works with no file at all.

| Variable | Local default | Used by |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` (the profile group adds `dev`) | API |
| `SERVER_PORT` | `38080` | API |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `jdbc:postgresql://localhost:55432/elimika`, `elimika`, `elimika-local` | API |
| `APP_KEYCLOAK_SERVERURL`, `APP_KEYCLOAK_REALM` | `http://localhost:58080`, `elimika-local` | API (admin client, JWKS) |
| `APP_KEYCLOAK_ADMIN_CLIENTID` / `_CLIENTSECRET` | `elimika-backend`, `elimika-local-backend-secret` | API |
| `APP_BOOTSTRAP_ADMIN_ENABLED` / `_EMAIL` / `_FIRST_NAME` / `_LAST_NAME` | `true`, `qa-admin@elimika.local`, `Ada`, `Admin` | API |
| `ENCRYPTION_SECRET_KEY`, `ENCRYPTION_SALT` | local constants | API |
| `SEARCH_MEILISEARCH_HOST`, `SEARCH_MEILISEARCH_API_KEY` | `http://localhost:57700`, the master key | API |
| `STORAGE_PATH`, `STORAGE_BASE_URL` | `~/elimika-local-storage`, `http://localhost:38080` | API |
| `API_URL`, `KEYCLOAK_URL`, `KEYCLOAK_UI_CLIENT_ID`, `KEYCLOAK_UI_CLIENT_SECRET`, `QA_PASSWORD` | as above, `Passw0rd!` | scripts |

`application-local.yaml` also sets `search.enabled=true`, `search.auto-rebuild=true`, every
`search.read-enabled.<index>=true` (`course_content` included), and switches the nightly jobs off
(`search.full-rebuild-cron=-`, `course.features.refresh-cron=-`); the seed runs both on demand.

## Users

Password for every user: `Passw0rd!`. Keycloak ids are fixed (`00000000-0000-4000-8000-0000000000NN`).

| User | Role in the app | Notes |
|---|---|---|
| `qa-admin@elimika.local` | platform admin | via the bootstrap runner |
| `qa-orgadmin@elimika.local` | organisation manager | admin of *Nairobi Code Academy* (created it) |
| `qa-instructor@elimika.local` | instructor, verified | member of Nairobi Code Academy (Westlands branch); opted in to location search, point about 1 km from Nairobi CBD; skills `JS` (resolves to JavaScript) and Python; approved to train JavaScript Fundamentals, Python, SQL, Web Accessibility |
| `qa-instructor2@elimika.local` | instructor, **unverified** | member of *Mombasa Data School*; opted in but never locatable |
| `qa-creator@elimika.local` | course creator, verified | owns every course except Kotlin |
| `qa-creator2@elimika.local` | course creator, verified | owns *Kotlin for Android*; admin of Mombasa Data School; "another creator" in the checks |
| `qa-student@elimika.local` | student, adult | enrolled (through the API) in *JavaScript Bootcamp Nairobi*, so affiliated with the org and branch; skill goals Python + SQL; reviewed JavaScript Fundamentals |
| `qa-minor@elimika.local` | student, born 2014 | enrolled in SQL Essentials (completed) and Web Accessibility; skill goal Python |
| `qa-guardian@elimika.local` | guardian | `PARENT` link to the minor with `ACADEMICS` share |
| `qa-outsider@elimika.local` | student | no enrolments, memberships or links |

## Seeded data

| What | Details |
|---|---|
| Organisations | Nairobi Code Academy (verified; branch *Westlands Campus* at -1.2676, 36.8108), Mombasa Data School (verified; branch *Mombasa Island*) |
| Categories / levels | Programming → Web Development (child), Data Science; Beginner, Intermediate, Advanced |
| Skills | JavaScript (aliases JS, ECMAScript), Python (py), SQL, React (ReactJS) |
| Courses | Published + approved: JavaScript Fundamentals, Advanced JavaScript Patterns (prerequisite: JavaScript Fundamentals), Python for Data Analysis, SQL Essentials, Kotlin for Android; *Web Accessibility Basics* (published, with a pending shadow-draft edit); *Algorithmic Trading with Python* (age 18+); *Rust Systems Draft* (draft); *Legacy jQuery Widgets* (archived). Each has two lessons with text content, a quiz and an assignment |
| Course skills | JS → JavaScript; Advanced JS → JavaScript, React; Python → Python, SQL; SQL → SQL; Trading → Python |
| Rubrics | Public Project Rubric (Chloe), Private Chloe Rubric, Private Carl Rubric |
| Training approvals | qa-instructor and Nairobi Code Academy for JS, Python, SQL, Web; qa-instructor2 and Mombasa Data School for Python |
| Classes | *JavaScript Bootcamp Nairobi* (PUBLIC, IN_PERSON), *SQL Private Cohort* (PRIVATE, IN_PERSON), *Python Online Evenings* (PUBLIC, ONLINE). In-person classes take the branch pin |
| Jobs | *JavaScript Weekend Trainer* (OPEN; requires JavaScript, mandatory, and React, optional), *SQL Evening Trainer* (FILLED), *Python Mombasa Trainer* (OPEN, other org, ONLINE, inherits Python + SQL from its course) |
| Co-enrolment | 8 synthetic adult learners (`adultNN@learners.elimika.local`, no Keycloak account): 6 share JS + Python (stored pair, k=6), 6 plus the minor share SQL + Web (k=7 with a minor < 10, never stored) |

**SQL, not the API, for:** the synthetic learners and their `course_enrollments` history (no Keycloak
accounts, back-dated enrolments), the minor's two course enrolments (so the SQL + Web pair is exactly 7
learners including a minor), and the FILLED job status (reaching it needs a hire). Everything else goes through
the API as the matching user. The seed also widens the migration-seeded onboarding age gate from 5-18 to 5-99
through `PUT /api/v1/system-rules/{uuid}`, because adult learners cannot create a student profile otherwise.

**Local-only trigger.** `POST /api/v1/admin/recommendations/features/refresh` (platform admin, registered only
under the `local` profile) runs the nightly `CourseFeatureRefreshJob` on demand.

## Reseeding

`scripts/local/seed.sh` is idempotent: it looks up everything it would create first. To start over:

```bash
scripts/local/down.sh --wipe && scripts/local/up.sh
```

After editing `docker/keycloak/elimika-local-realm.json`, run `scripts/local/down.sh --reset-keycloak` and
`up.sh` again (the realm is imported only when it does not exist). The users get new local `users` rows on their
next request only if their Keycloak id changed; ids are fixed in the JSON, so they normally do not.

## Smoke suite

`scripts/local/smoke.sh` logs in as every role and checks the rule matrix: anonymous global search scope,
draft and shadow hiding, typo tolerance, `/content` and `/similar` for public versus draft courses, `_like`
400s, people search scoped to managed organisations (by name; exact email on the roster) with immediate
drop-out on a revoked membership (the suite flips the mapping row in SQL and restores it), candidate fit
summaries, non-OPEN jobs for the owning organisation, job matches with eligibility, unverified and opted-out
instructors hidden (the suite opts qa-instructor out and back in), near-me bands with no raw coordinates,
recommendation exclusions and reasons, the minor's age band and the k ≥ 10 co-enrolment rule, guardian access,
creator-only drafts and skill tagging, prerequisite cycles, rubric privacy, admin index endpoints, query
redaction in `request_audit_log`, and the search-down behaviour (it stops Meilisearch, checks the 503s and
the SQL paths, and starts it again).

`PENDING` marks a documented, not-yet-wired feature: `q=js` finding a JavaScript-tagged job needs skill
aliases as Meilisearch synonyms (`skills-taxonomy.md`). It is reported but does not fail the run.

## UI

In the `elimika-ui` repo: `scripts/local-dev.sh` copies `.env.local.example` to `.env.local` (fresh
`AUTH_SECRET`), checks the API and the realm, and runs `pnpm dev`. Sign in at http://localhost:3000 as any user
above. `node scripts/local-login-check.mjs` signs in headlessly as qa-student and calls the API through the UI
proxy.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `up.sh` stops at "backend exited" | Read `build/local/backend.log`. A port in use (38080) or a JDK other than 21 are the usual causes |
| Every API call is 401 | The token's issuer realm or JWKS is wrong: check `APP_KEYCLOAK_SERVERURL` / `APP_KEYCLOAK_REALM`. Tokens must come from `http://localhost:58080` (the realm forces that hostname) |
| A user's first request is 500 / the user has no row | `UserSyncFilter` could not read the user through the admin API: the `elimika-backend` service account lost its realm-management roles. `down.sh --reset-keycloak` |
| `q` answers 503 "Search is unavailable" | Meilisearch is down or an index is not read-enabled: `docker compose -f docker/compose.local.yaml ps`, then `GET /api/v1/admin/search/indexes` as qa-admin; rebuild with `seed.sh` |
| Search results look stale after editing rows in SQL | SQL writes fire no entity triggers. Re-run `seed.sh` (it rebuilds every index) or `POST /api/v1/admin/search/indexes/{index}/documents/{uuid}/sync` |
| Recommendations show no co-enrolment reasons | The feature tables are filled by the refresh, not live: re-run `seed.sh` |
| "Student above maximum age of 18" | The age gate was reset (fresh database). `seed.sh` widens it again |
| Keycloak realm changes do not apply | The realm is imported once: `down.sh --reset-keycloak` |
| UI login loops back to the sign-in page | `.env.local` points elsewhere, or `AUTH_URL` is not `http://localhost:3000` (the redirect URI the client allows) |
