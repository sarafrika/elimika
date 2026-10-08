# Instructor apply-to-train

Instructors apply to train a course **or** a program in five steps:
1. Choose it.
2. Define learner groups, each with a lesson plan.
3. Set delivery methods and rates.
4. Answer the requirements.
5. Review and submit.

Organisations keep their own wizard (classrooms, requirements, pricing, review). Every rule below that mentions learner groups applies to instructors only.

- **Rates:** per hour and per day only. Per-session pricing is retired (details below).
- **Catalogue:** the picker lists courses and programs together. Meilisearch does the merge, the counts and the facets. SQL only re-checks visibility and attaches the caller's own data: their application status and the minimum fee.
- **Learner groups:** named age bands. Each group has hours for every active lesson of the course, or of every course in a program.

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

 2 Learner groups + lesson plan ─ GET /api/v1/courses/{uuid}/lessons ─────────────► lessons (active only)
   (program: lessons of each member course, in ProgramCourse sequence)

 3 Delivery & rates (hourly + daily per method)
 4 Requirements (course: answer; program: read-only)
 5 Review ─ POST /api/v1/courses/{uuid}/training-applications ─────────────────────► course_training_applications (8 rate cells)
            POST /api/v1/programs/{uuid}/training-applications                       program_training_applications
            body: rate_card, requirement_answers, application_notes,               training_application_requirement_answers
                  learner_groups[{name, min_age, max_age,                          training_application_learner_groups
                                  lesson_hours[{lesson_uuid, hours}]}]             training_application_lesson_hours (FK cascade)
            PUT …/{applicationUuid} (pending only; omitted learner_groups = keep)
            DELETE …/{applicationUuid} (withdraw: groups and hours deleted)

 Course creator review ─ GET …/training-applications/{uuid} ─► learner_groups[] with lesson titles and total_hours
```

## Learner group rules (`TrainingApplicationLearnerGroups`)

| Rule | Detail |
|---|---|
| Who | Instructor applicants only. An organisation that sends any groups gets a 400: "Only instructor applicants can define learner groups". |
| Omitted vs empty | `learner_groups` omitted (null) keeps what is stored. For an instructor, `[]` is refused: at least one group is required. |
| Count | At most 10 groups. |
| Names | Required, at most 80 characters, unique ignoring case. |
| Ages | `min_age <= max_age`. Groups must not overlap, but gaps are allowed. |
| Course age range | Every group must sit inside `courses.age_lower_limit..age_upper_limit`. A null limit is open. |
| Program age range | The intersection of the member courses' ranges: highest lower limit to lowest upper limit. If the courses share no age, the application is refused. |
| Lessons | Every active lesson needs hours in the range 0 < hours ≤ 24, at most 2 decimals. Unknown or duplicate lessons are refused. |

Response field `learner_groups[]` has this shape:

```
{uuid, name, min_age, max_age, total_hours,
 lesson_hours[{lesson_uuid, course_uuid, lesson_title, lesson_number, hours}]}
```

`lesson_title` is null when a lesson has since been removed. Non-parties get `learner_groups: null`.

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
