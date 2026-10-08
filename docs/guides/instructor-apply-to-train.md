# Instructor apply-to-train

Instructors apply to train a course **or** a program in five steps:
1. Choose it.
2. Define age groups, each with a lesson plan.
3. Set delivery methods and rates.
4. Answer the requirements.
5. Review and submit.

Organisations keep their own wizard (classrooms, requirements, pricing, review). Every rule below that mentions age groups on an application applies to instructors only.

- **Rates:** per hour and per day only. Per-session pricing is retired (details below).
- **Catalogue:** the picker lists courses and programs together. Meilisearch does the merge, the counts and the facets. SQL only re-checks visibility and attaches the caller's own data: their application status and the minimum fee.
- **Age groups:** named age bands. On an application, each group has hours for every active lesson of the course, or of every course in a program. Instructors and organisations can also keep saved age groups to start from.

## Flow

```
 UI (instructor)                         API                                               storage
 ───────────────                         ───                                               ───────
 1 Choose a course or program ── GET /api/v1/catalogue/apply-to-train ─► ApplyCatalogueSearchService (course)
   cards, search, filters        ?q=&show=&category_uuid=&fit=open|skills|applied   courses + programs indexes (Meilisearch):
   (no dropdown)                 (instructor domain, APPROVED mapping only)           public scope ANDed into every query,
                                                                                      fit = uuid IN/NOT IN caller's applications,
                                                                                      skill_uuids IN caller's wallet skills;
                                                                                      facets: show, category, fit counts
                                                                                    SQL re-check ─► courses / training_programs
                                                                                    my_application ─► course/program_training_applications
                                                                                    minimum_training_fee ─► courses (never indexed)

 2 Age groups + lesson plan ──── GET /api/v1/courses/{uuid}/lessons ─────────────► lessons (active only)
   (program: lessons of each member course, in ProgramCourse sequence)
   "Add from saved" ──────────── GET /api/v1/instructors/me/age-group-presets ──────► age_groups (owner INSTRUCTOR = caller,
                                                                                      ORGANISATION = caller's organisations);
                                                                                      copied into the application, never linked

 3 Delivery & rates (hourly + daily per method)
 4 Requirements (course: answer; program: read-only)
 5 Review ─ POST /api/v1/courses/{uuid}/training-applications ─────────────────────► course_training_applications (8 rate cells)
            POST /api/v1/programs/{uuid}/training-applications                       program_training_applications
            body: rate_card, requirement_answers, application_notes,               training_application_requirement_answers
                  age_groups[{name, min_age, max_age,                              age_groups (owner COURSE_/PROGRAM_TRAINING_APPLICATION)
                              lesson_hours[{lesson_uuid, hours}]}]                 age_group_lesson_hours (FK cascade)
            PUT …/{applicationUuid} (pending only; omitted age_groups = keep)
            DELETE …/{applicationUuid} (withdraw: groups and hours deleted)

 Course creator review ─ GET …/training-applications/{uuid} ─► age_groups[] with lesson titles and total_hours
```

## Age group rules on an application (`AgeGroups`, policy in `TrainingApplicationAgeGroups`)

| Rule | Detail |
|---|---|
| Who | Instructor applicants only. An organisation that sends any groups gets a 400: "Only instructor applicants can define age groups". |
| Omitted vs empty | A new instructor submission must send `age_groups`. On an update, omitting it keeps what is stored. For an instructor, `[]` is refused: at least one group is required. |
| Count | At most 10 groups. |
| Names | Required, at most 80 characters, unique ignoring case. |
| Ages | `min_age <= max_age`. Groups must not overlap, but gaps are allowed. |
| Course age range | Every group must sit inside `courses.age_lower_limit..age_upper_limit`. A null limit is open. |
| Program age range | The intersection of the member courses' ranges: highest lower limit to lowest upper limit. If the courses share no age, the application is refused. |
| Lessons | Every active lesson needs hours in the range 0 < hours ≤ 24, at most 2 decimals. Unknown or duplicate lessons are refused. |

Response field `age_groups[]` has this shape:

```
{uuid, name, min_age, max_age, total_hours,
 lesson_hours[{lesson_uuid, course_uuid, lesson_title, lesson_number, hours}]}
```

`lesson_title` is null when a lesson has since been removed. Non-parties get `age_groups: null`.

## Student groups vs age groups

- **Student groups** (tenancy `student_groups`) are rosters of real students, filed by branch, tier and stream. A marketplace job's "Student groups" (`target_group_uuids`) picks from them. They have no ages.
- **Age groups** (course `age_groups`) are named age bands with no students. They are either saved by an owner or copied into a training application with a lesson plan.

## Saved age groups (`SavedAgeGroupService`)

| Endpoint | Who |
|---|---|
| `GET/POST /api/v1/instructors/me/age-groups` | the calling instructor (APPROVED instructor domain) |
| `GET /api/v1/instructors/me/age-group-presets` | the calling instructor: their own groups, then those of every organisation they currently belong to (with `owner_name`) |
| `GET /api/v1/organisations/{uuid}/age-groups` | members of the organisation |
| `POST /api/v1/organisations/{uuid}/age-groups` | managers of the organisation |
| `PUT/DELETE /api/v1/age-groups/{uuid}` | the owner only (its instructor, or the organisation's managers); application copies are not reachable here |

Rules: name required, at most 80 characters, unique ignoring case per owner (a duplicate gets 409); 0 ≤ min ≤ max ≤ 120; at most 50 per owner. Overlaps are allowed, because an owner may keep several schemes. Editing or deleting a saved group never changes an application that copied it.

## Rates: per hour and per day

- A rate card holds 8 cells (4 methods × hourly and daily). An offered method must price both cells, each at or above the minimum training fee.
- Migration `V202610081310` dropped the `*_session_rate` columns from:
  - `course_training_applications`
  - `program_training_applications`
  - `course_training_rate_updates`
  - `program_training_rate_updates`
- A CSV copy of the staging values was taken first (`/root/backups/session_rates_*_20261008.csv` on the staging host).
- `RateBasis.PER_SESSION` stays only so legacy `class_definitions`, `class_marketplace_jobs` and `bookings` rows still load. New jobs, bookings and class definitions cannot use it, and neither can a class switching basis: `RateBasis.requireSelectable` rejects it.
- A legacy per-session class keeps its own `sale_price` and `instructor_pay`, so learner charges and payouts are unchanged.

## Catalogue response (`ApplyCatalogueResponse`)

- `content[]`:
  - `type` (course | programme), `uuid`, `title`, `code`
  - `category_uuids`, `category_names`, `creator_name`, `thumbnail_url`
  - `age_lower_limit`, `age_upper_limit`, `lesson_count`, `requirement_count`, `course_count`
  - `matches_skills`
  - `my_application {uuid, status}`
  - `minimum_training_fee`
- `metadata`: standard paging.
- `facets`:
  - `show {all, courses, programmes}`
  - `category [{uuid, name, count}]`
  - `fit {open, skills, applied}`

"Applied" counts an application in any status. When search is unavailable the endpoint returns 503 "Search is unavailable". There is no SQL fallback.
