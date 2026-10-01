# Skills taxonomy and tagging

One admin-curated list of skills, used to tag courses (by their creators), marketplace jobs (by the
posting organisation) and, automatically, instructor skills. **Tagging is optional and never blocks
publishing.** A job with no tags of its own inherits its course's skills.

- **Module `skills`** is a leaf (`allowedDependencies = {"shared"}`). It owns the `skills` table and exposes
  `skills :: skills-spi` (`SkillLookupService`: `findByUuids`, `findBySlugs`, `resolve(names)`, `aliases()`;
  `SkillSlugs.slugify`). Tag tables live in the modules that own the tagged rows.
- **No SQL text search.** `GET /api/v1/skills?q=` filters the small taxonomy in memory.

## Flow

```
 UI                                   API                                          storage
 ──                                   ───                                          ───────
 Admin screen ── POST/PUT/DELETE /api/v1/admin/skills ──► SkillService (skills) ──► skills
                 (platform admin only)                    slug unique, aliases unique, no parent cycles
                                                          └ SearchSynonymsChanged("skills") (after commit)
                                                              ─► search re-applies synonyms to courses,
                                                                 marketplace_jobs, instructors settings

 Tag picker ──── GET /api/v1/skills?q=&limit= ──────────► SkillService.listActive
                 (signed in)                               active only, matched in memory
                                                           (exact name/slug/alias, then prefix, then contains)

 Course editor ─ PUT /api/v1/courses/{uuid}/skills ─────► CourseSkillServiceImpl (course) ─► course_skills
                 (course owner only)                       skills validated via skills-spi
                 GET  …/skills (anyone who can read        │ JPA trigger ─► courses index: skill_uuids
                 the course)                               └ CourseSkillsChangedEvent
                                                                 │ (after commit)
                                                                 ▼
                                                           CourseSkillsChangeIndexer (classes)
                                                           re-indexes the course's jobs

 Job editor ──── PUT /api/v1/classes/jobs/{jobUuid}/required-skills ─► ClassMarketplaceJobRequiredSkillServiceImpl
                 GET …/required-skills                      (classes) ─► class_marketplace_job_required_skills
                 (managers of the posting organisation      │ none of its own? JobRequiredSkills falls back to
                  or platform admin)                        │ course_skills via course-spi (inherited: true)
                                                            └ JPA trigger ─► marketplace_jobs index:
                                                              required_skill_uuids, required_skill_names

 Instructor profile ─ POST/PUT /api/v1/instructors/{uuid}/skills ─► InstructorSkillServiceImpl (instructor)
                      skill_name (free text)                    resolve(name) via skills-spi
                                                                ─► instructor_skills.skill_uuid (or null)
                                                                ─► instructors index: skill_uuids
```

## Schema

| Table | Columns | Notes |
|---|---|---|
| `skills` | `uuid`, `name`, `slug` (UNIQUE, `^[a-z0-9]+(-[a-z0-9]+)*$`), `parent_uuid` (self FK, `SET NULL`), `aliases text[]`, `active`, audit | Seeded from the distinct `instructor_skills` and `course_creator_skills` names in use, one per slug, named by the most used spelling |
| `instructor_skills.skill_uuid` | `UUID NULL` → `skills` (`SET NULL`) | Backfilled by slug; names that match nothing keep their free text and no link |
| `course_skills` | `course_uuid` (FK, cascade), `skill_uuid` (FK, cascade), `level` (BEGINNER..EXPERT, `ProficiencyLevelConverter`), `weight` 1-5 | UNIQUE (course, skill) |
| `class_marketplace_job_required_skills` | `job_uuid` (FK, cascade), `skill_uuid` (FK, cascade), `min_proficiency` (BEGINNER..EXPERT), `is_mandatory` | UNIQUE (job, skill) |
| `learner_skill_goals` | `student_uuid` (FK, cascade), `skill_uuid` (FK, cascade), `source` (SELF/GUARDIAN/ADMIN), audit | UNIQUE (student, skill). Set by the learner via `PUT /api/v1/students/{uuid}/skill-goals`; drives `SKILL_GAP` course recommendations (see `course-recommendations.md`) |

**Slug rule** (`SkillSlugs.slugify`, and the same SQL in the migration): lower-case, every run outside
`[a-z0-9]` becomes one hyphen, hyphens trimmed. `"Java  Programming!"` → `java-programming`. A name
with no ASCII letter or digit has no slug and is never seeded or linked.

**Resolution** (`resolve(names)`): an active skill whose slug equals the name's slug, else one whose
alias's slug does. Instructor skills are linked on create, and re-resolved on update when the name
changes (an unchanged name keeps its link, even to a since-retired skill).

## API

Proficiency values are returned lower-case (`beginner`, `intermediate`, `advanced`, `expert`) and
accepted in any case.

### `GET /api/v1/skills?q=&limit=` (signed in)

Active skills, `[{uuid, name, slug, parent_uuid, aliases, active, created_date, updated_date}]`.
`q` ≤ 200 characters; `limit` 1-500, default 50.

### `/api/v1/admin/skills` (platform admin)

| Method | Path | Does |
|---|---|---|
| `GET` | `?q=&active=` | Whole taxonomy, name order |
| `GET` | `/{uuid}` | One skill |
| `POST` | | `{name, slug?, parent_uuid?, aliases?, active?}` → 201; 409 when the slug or an alias already names another skill |
| `PUT` | `/{uuid}` | Replaces every field. `active=false` retires a skill: existing tags keep it, it can no longer be newly picked |
| `DELETE` | `/{uuid}` | 204. Drops it from every course and job, unlinks instructor skills (their text stays). Prefer retiring |

### `GET|PUT /api/v1/courses/{uuid}/skills`

`GET` for anyone who can read the course (404 otherwise). `PUT` for the course's owner only
(`@courseSecurityService.isCourseOwner`), body `{"skills": [{"skill_uuid", "level"?, "weight"?}]}` - the complete
list, `[]` clears. 400 for an unknown or duplicate skill, a newly added retired skill, or a shadow draft
(tags go on the live course). Response: `[{skill_uuid, skill_name, skill_slug, level, weight, skill_active}]`.

### `GET|PUT /api/v1/classes/jobs/{jobUuid}/required-skills`

Managers of the posting organisation and platform admins, for both read and write (403 otherwise).
Body `{"skills": [{"skill_uuid", "min_proficiency"?, "is_mandatory"?}]}`, `[]` clears (and the job
inherits again). Response:

```json
{ "job_uuid": "…", "inherited": true, "inherited_from_course_uuid": "…",
  "skills": [ { "skill_uuid": "…", "skill_name": "Kubernetes", "skill_slug": "kubernetes",
                "min_proficiency": "advanced", "is_mandatory": true, "inherited": true, "skill_active": true } ] }
```

Inherited skills are all mandatory, with the course's `level` as `min_proficiency`.

## Search

See [search-platform.md](search-platform.md#skill-tags-in-the-indexes): `instructors`, `courses` and
`marketplace_jobs` moved to schema version 2 and must be rebuilt after deploy.

**Names and aliases are search synonyms** on those three indexes
([details](search-platform.md#skill-names-and-aliases-as-synonyms)). `SkillSearchSynonyms` (skills module,
implementing `shared.search.SearchSynonymSource` named `skills`) maps every active skill's name and aliases,
lower-cased, to each other: "JavaScript" with alias "JS" makes `q=js` find a job tagged JavaScript. Every
create, update or delete publishes `SearchSynonymsChanged("skills")`, and the search module re-applies the
settings of the affected indexes after commit - no rebuild needed.
