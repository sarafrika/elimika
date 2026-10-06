# Assessment and Grading

## Model

| Concept | Storage | Notes |
|---|---|---|
| Course component (Attendance 10%, Practical 40% ...) | `course_assessments` | `weight_percentage`; active weights must total exactly 100% when the course is published or an edit is approved |
| Per-lesson component | `course_assessments.per_lesson = true` | graded lesson by lesson through the evaluation plan |
| Cell (lesson x component) | `course_assessment_line_items.lesson_uuid` | one per lesson per component; carries the rubric and optionally that lesson's quiz or assignment. No cell = "None" |
| Rubric | `assessment_rubrics`, `rubric_criteria`, `rubric_scoring_levels` | standard levels: Distinction 5, Merit 4, Pass 3 (passing), Fail 2, No Effort 1 |
| Pass mark | `courses.pass_mark`, `training_programs.pass_mark` | percentage of the final grade |
| Result | `course_enrollments.result_status`, `program_enrollments.result_status` | `IN_PROGRESS` until every required item is graded, then `PASSED` / `FAILED` |
| Program component | `program_assessments` | course components feed one through `course_assessments.program_assessment_uuid` |
| Session's lesson | `scheduled_instances.lesson_uuid` | optional; otherwise session N grades lesson N |

## UI ↔ API ↔ Storage Flow

```text
Course creator: grading design
  | POST /api/v1/courses/{courseUuid}/assessments          { title, assessment_type, weight_percentage,
  |                                                          per_lesson, sync_class_attendance, rubric_uuid,
  |                                                          program_assessment_uuid }
  | POST /api/v1/rubrics/{rubricUuid}/scoring-levels/standard   five standard levels
  | GET  /api/v1/courses/{courseUuid}/evaluation-plan       lessons x per-lesson components grid
  | PUT  /api/v1/courses/{courseUuid}/evaluation-plan       { cells: [{ lesson_uuid, assessment_uuid,
  |                                                          enabled, rubric_uuid, quiz_uuid, assignment_uuid }] }
  | PUT  /api/v1/courses/{uuid}  { pass_mark, course_code }
  v
course_assessments / course_assessment_line_items (cells) / courses.pass_mark

Instructor: delivery
  | PATCH /api/v1/timetable/schedule/{instanceUuid}/lesson?lessonUuid=   optional session -> lesson
  | PATCH /api/v1/enrollment/{enrollmentUuid}/attendance                 attendance marked
  |     -> AttendanceMarkedEventDTO (session_number, lesson_uuid)
  |     -> per-lesson attendance component: the lesson's cell is scored
  |        (attending any session of a lesson counts; a later absence never undoes it)
  | gradebook line-item scores and rubric evaluations (existing endpoints)
  v
course_assessment_line_item_scores / rubric evaluations
  -> course_assessment_scores (per component) -> course_enrollments.final_grade
  -> result: every required item graded ? (final_grade >= pass_mark ? PASSED : FAILED) : IN_PROGRESS
     PASSED completes the enrolment (certificates need COMPLETED); attendance alone no longer does
  -> CourseGradeRecalculatedEvent

Program
  | POST/GET/PUT/DELETE /api/v1/programs/{uuid}/assessments   weighted program components (100% to publish)
  v
program_enrollments.final_grade = sum over components of (average linked course scores x weight) / weights graded
  (no components: average of member course final grades)
  result decided once every required course has a result; pass mark null -> all required courses passed
```

Read the result on `GET /api/v1/courses/{courseUuid}/gradebook/enrollments/{enrollmentUuid}` (`pass_mark`, `result_status`).
