# Summary

This PR introduces a weighted course gradebook model built around course assessment components and nested line items.

The implementation separates three concerns clearly:

- `course_assessments` act as top-level gradebook components that contribute to the final course grade.
- `course_assessment_line_items` act as the actual graded items inside a component.
- rubrics remain attached to line items for grading criteria, without carrying final-grade weighting themselves.

This aligns the backend with the gradebook model discussed for attendance, projects, discussions, participation, assignments, quizzes, exams, and other graded work.

# Linked Issue

- N/A

# Changes

## Schema and Persistence

- added `aggregation_strategy` to `course_assessments`
- added `course_assessment_line_items` table for component-scoped gradebook items
- added `course_assessment_line_item_scores` table for per-enrollment line-item scores
- added database constraints for:
  - valid aggregation strategies
  - valid line-item types
  - one optional source link per line item (`assignment_uuid` or `quiz_uuid`)
  - positive score and weight bounds
- added JPA entities, repositories, enums, and attribute converters for the new gradebook structures

## Gradebook Domain Model

- introduced component aggregation strategies:
  - `points_sum`
  - `weighted_average`
- allowed line-item categories beyond assignments and quizzes:
  - attendance
  - project
  - discussion
  - exam
  - practical
  - performance
  - participation
  - manual
- kept `assignment_uuid` and `quiz_uuid` as optional source links so assignments and quizzes behave as linked task sources rather than the only gradebook item types

## Application Logic

- added gradebook service support for:
  - creating, listing, updating, and deleting line items
  - upserting manual line-item scores
  - recalculating component aggregates
  - recalculating enrollment final grades
  - building enrollment gradebook views
- integrated assignment grading into gradebook synchronization
- integrated graded quiz attempts into gradebook synchronization
- fixed derived component-score clearing when all active line items for a component are removed

## Validation Rules

- validates that total `course_assessments.weight_percentage` per course does not exceed `100%`
- validates that weighted components require positive line-item weights
- validates assignment and quiz links belong to the same course
- validates that a line item cannot reference both an assignment and a quiz
- keeps rubric weighting independent from gradebook weighting

## API Surface

- extended course assessment payloads with `aggregation_strategy`
- added dedicated gradebook endpoints for:
  - line-item CRUD
  - line-item score upsert
  - enrollment gradebook retrieval
- updated line-item DTO display semantics so `item_type` represents the academic gradebook category instead of only the backing task source

# Tests

## Executed

- `gradle test --tests "apps.sarafrika.elimika.course.service.impl.CourseGradebookServiceImplTest"`

## Added Coverage

- weighted-average aggregation across multiple line items
- final-grade recomputation after line-item scoring
- assignment grade synchronization into derived line-item scores
- validation failure when a weighted component is given an unweighted line item
- support for non-assignment categories backed by linked tasks, such as a `project` line item referencing an assignment
- clearing stale derived component scores when line items no longer exist

## Full Suite Note

- `gradle test` still reports pre-existing Spring Modulith architecture violations unrelated to this gradebook feature
- current failures are in:
  - `course <-> timetabling` module cycle
  - `revenue` module dependency violations

# Configuration / Secret Impacts

- no new secrets introduced
- no environment variable changes required
- includes one Flyway migration:
  - `V202603160619__add_course_gradebook_line_items.sql`

# Migration and Rollout Notes

- run Flyway migrations before exercising the new gradebook endpoints
- existing course assessments default to `points_sum`
- line-item weights are only used when the parent component uses `weighted_average`
- rubric criteria remain equally weighted inside a rubric; only gradebook components and line items are weighted in this feature

# Reviewer Notes

- review the aggregation policy choice per component:
  - `points_sum` for total-points accumulation
  - `weighted_average` for explicitly weighted line items
- review whether line-item weight totals inside a weighted component should be capped to `100%` as an additional business rule; current implementation normalizes by total configured weight, so calculation remains correct even when totals differ from `100`
