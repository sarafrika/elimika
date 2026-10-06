# Course Grading Setup (for the course creation flow)

How the UI should set up assessments, rubrics and per-lesson grading while a course is built, and
how grades are read back. Field names are the API's snake_case. Deeper model: `assessment-and-grading.md`.

## Flow at a glance

```text
1 Course basics ──► 2 Lessons ──► 3 Rubrics ──► 4 Components ──► 5 Lesson grid ──► 6 Publish
  pass_mark          (+ quizzes /    criteria x    weights = 100%     cells per lesson    weight check
  course_code         assignments)   levels                                              (409 if != 100)
                                                         │
                       Delivery: attendance / quiz / assignment / rubric marks ──► gradebook ──► result
```

| Step | Call | Notes |
|---|---|---|
| 1. Course | `POST /api/v1/courses` / `PUT /api/v1/courses/{uuid}` | Send `pass_mark` (0–100, final-grade %) and optional `course_code`. Do not send durations. |
| 2. Lessons | existing lesson, quiz and assignment endpoints | Lessons first: cells and quiz/assignment links point at them. |
| 3. Rubric | `POST /api/v1/rubrics` `{ title, description, rubric_type, course_creator_uuid, is_public }` | `rubric_type` e.g. Attendance, Practical, Quiz, Performance. |
| 3a. Levels | `POST /api/v1/rubrics/{rubricUuid}/scoring-levels/standard` | Distinction 5, Merit 4, Pass 3 (passing), Fail 2, No Effort 1. Custom: `POST .../scoring-levels/batch`. |
| 3b. Criteria | `POST /api/v1/rubrics/{rubricUuid}/criteria` `{ component_name, description, display_order }` | e.g. Technique, Tonal Quality, Rhythm, Pitch. |
| 3c. Descriptors | `PUT /api/v1/rubrics/{rubricUuid}/matrix/cells` `{ criteria_uuid, scoring_level_uuid, description, points }` | One per criterion x level. Check `GET .../matrix/ready` before using the rubric. |
| 3d. Attach | `POST /api/v1/courses/{courseUuid}/rubrics` `{ rubric_uuid, usage_context, is_primary_rubric }` | Makes the rubric readable to the course's instructors and learners. |
| 4. Components | `POST /api/v1/courses/{courseUuid}/assessments` | `{ title, assessment_type, weight_percentage, aggregation_strategy: "points_sum" or "weighted_average", rubric_uuid, is_required, per_lesson, sync_class_attendance }`. Attendance: `sync_class_attendance: true` + `points_sum`. |
| 5. Lesson grid | `GET` then `PUT /api/v1/courses/{courseUuid}/evaluation-plan` | `{ cells: [{ lesson_uuid, assessment_uuid, enabled, rubric_uuid, quiz_uuid, assignment_uuid }] }`. Only `per_lesson` components appear. `enabled: false` = "None". |
| 6. Publish | `POST /api/v1/courses/{uuid}/publish` | 409 unless active component weights total exactly 100%. Same check when an admin approves an edit to a live course. |

## Per-lesson rules the UI should respect

- Render the grid from `GET .../evaluation-plan`: rows = lessons in order, columns = `components`, each `cells[i]` is the line item or `null`.
- A cell's quiz or assignment must belong to that cell's lesson (400 otherwise); a cell's rubric overrides the component's.
- Attendance cells are scored automatically from class attendance. A session grades the lesson set with
  `PATCH /api/v1/timetable/schedule/{instanceUuid}/lesson?lessonUuid=`, otherwise session N grades lesson N.
  Attending any session of a lesson counts. If the plan has no cells yet, the first marked session creates them.
- Editing a published course edits its draft: use the draft's course uuid for steps 3d–5 while a draft is open.

## Grading during delivery (instructor gradebook)

| Action | Call |
|---|---|
| Manual / numeric score | `PUT /api/v1/courses/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/scores/{enrollmentUuid}` `{ score, max_score, comments }` |
| Rubric marking | `PUT .../line-items/{lineItemUuid}/rubric-evaluations/{enrollmentUuid}` `{ criteria_selections: [{ criteria_uuid, scoring_level_uuid, comments }], comments }` — one level per criterion; score = sum of level points |
| Read a learner's grade | `GET /api/v1/courses/{courseUuid}/gradebook/enrollments/{enrollmentUuid}` → `final_grade`, `pass_mark`, `result_status`, components and line items |

`result_status` stays `IN_PROGRESS` until every active item of every required component is graded, then
becomes `PASSED` (completes the enrolment, unlocks the certificate) or `FAILED`. Show "graded x of y" from
the gradebook's line items, and the pass mark next to the final grade.

## Programs

Define program components with `POST /api/v1/programs/{uuid}/assessments` (weights total 100% to publish), set
`pass_mark` on the program, and link each course component to its program component with
`program_assessment_uuid` in step 4. The program grade and result are computed automatically.
