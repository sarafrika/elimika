# Class-Level Assessment Scheduling Design

## Goals
- Give instructors control over when quizzes and assignments are available for the specific classes they deliver.
- Let trainers supplement the course blueprint with class-specific assessments while retaining the base course content as templates.
- Persist deadlines and release windows at the class level so downstream modules (notifications, analytics, grading) can reason about cohort-specific timelines.
- Maintain backward compatibility for existing course-scoped CRUD APIs while layering in class-aware behaviour.

## Vocabulary
- **Course template**: The canonical course/lesson/assessment definition maintained by course authors (current `assignments`/`quizzes` tables).
- **Class definition**: A cohort blueprint (`class_definitions`), linked to a course and owned by a training organisation.
- **Scheduled instance**: A concrete class run (`scheduled_instances`) with calendar times and the assigned instructor.
- **Class assessment schedule**: Class-scoped assignment/quiz timing, visibility windows, and override metadata.

## Proposed Schema Changes
All timestamps are stored in UTC; `*_at` columns default to `now()` only where explicitly called out.

### 1. Extend Assignment & Quiz Templates
Add lightweight metadata so we can distinguish between course templates and class-derived copies.

| Table | Column | Type | Notes |
|-------|--------|------|-------|
| `assignments` | `scope` | `VARCHAR(32)` | Values: `COURSE_TEMPLATE`, `CLASS_CLONE`. Defaults to `COURSE_TEMPLATE`. |
| `assignments` | `class_definition_uuid` | `UUID` | Nullable; populated when `scope = CLASS_CLONE`. |
| `assignments` | `source_assignment_uuid` | `UUID` | Nullable self-reference; identifies the course template that was cloned. |
| `quizzes` | `scope` | `VARCHAR(32)` | Same semantics as assignments. |
| `quizzes` | `class_definition_uuid` | `UUID` | Nullable pointer to the owning class. |
| `quizzes` | `source_quiz_uuid` | `UUID` | Nullable self-reference to the base template. |

Add indexes on (`class_definition_uuid`, `scope`) for both tables to keep lookups fast.

### 2. Class Assignment Schedule Table
```text
class_assignment_schedules (
    uuid UUID PRIMARY KEY,
    class_definition_uuid UUID NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    lesson_uuid UUID NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    assignment_uuid UUID NOT NULL REFERENCES assignments (uuid) ON DELETE CASCADE,
    visible_at TIMESTAMP WITH TIME ZONE NULL,
    due_at TIMESTAMP WITH TIME ZONE NULL,
    grading_due_at TIMESTAMP WITH TIME ZONE NULL,
    timezone VARCHAR(64) NULL,
    release_strategy VARCHAR(32) NOT NULL DEFAULT 'INHERITED', -- INHERITED | CUSTOM | CLONE
    max_attempts INTEGER NULL,
    instructor_uuid UUID NOT NULL REFERENCES instructors (uuid),
    notes TEXT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(255) NOT NULL,
    updated_date TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_class_assignment UNIQUE (class_definition_uuid, assignment_uuid)
);
```

The `release_strategy` flag captures whether we are inheriting course-level defaults, overriding them, or pointing to a class-specific clone.

### 3. Class Quiz Schedule Table
Structure mirrors assignments with quiz-specific overrides:

```text
class_quiz_schedules (
    uuid UUID PRIMARY KEY,
    class_definition_uuid UUID NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    lesson_uuid UUID NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    quiz_uuid UUID NOT NULL REFERENCES quizzes (uuid) ON DELETE CASCADE,
    visible_at TIMESTAMP WITH TIME ZONE NULL,
    due_at TIMESTAMP WITH TIME ZONE NULL,
    timezone VARCHAR(64) NULL,
    release_strategy VARCHAR(32) NOT NULL DEFAULT 'INHERITED', -- INHERITED | CUSTOM | CLONE
    time_limit_override INTEGER NULL,
    attempt_limit_override INTEGER NULL,
    passing_score_override NUMERIC(5,2) NULL,
    instructor_uuid UUID NOT NULL REFERENCES instructors (uuid),
    notes TEXT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(255) NOT NULL,
    updated_date TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_class_quiz UNIQUE (class_definition_uuid, quiz_uuid)
);
```

### 4. Indices & Materialized Views
- `idx_assignment_schedule_due_at` for notification queries.
- `idx_quiz_schedule_due_at` similarly.
- Consider a future read-optimized projection (materialized view) joining schedules with templates to serve the classroom UI.

## Persistence Strategy
1. **Flyway migrations**: Introduce new versioned scripts for each structural change. Existing assignment/quiz rows default to `scope = COURSE_TEMPLATE`.
2. **Entity mappings**:
   - New JPA entities under `apps.sarafrika.elimika.classes.model` for `ClassAssignmentSchedule` and `ClassQuizSchedule`.
   - Enums for `AssignmentScope`, `QuizScope`, and `ClassAssessmentReleaseStrategy`.
   - Attribute converters for new enums to keep column values predictable.
3. **Repositories & factories**: Create Spring Data repositories and DTO/factory pairs mirroring existing module conventions.
4. **Audit integration**: Reuse `DatabaseAuditListener` so new tables automatically receive `created_by`/`updated_by` from the authenticated principal.

## Open Questions
1. Should custom instructor-created assessments live in the `classes` module instead of `course`?
2. Do we need per-scheduled-instance overrides or is class-definition granularity enough?
3. How do we expose deadline updates to notifications and analytics?

## Next Steps
1. Finalise enum names and placement (course vs shared module).
2. Draft Flyway scripts implementing the schema deltas above.
3. Update domain models and DTOs, then wire service APIs for instructor actions.
4. Extend notification triggers to consume `due_at` from the new tables.

## Service & API Outline

### Service Layer Responsibilities
- **ClassAssessmentScheduleService**
  - Manage both assignment and quiz schedules to keep shared rules in one place.
  - Provide CRUD operations bound to instructor ownership checks.
  - Resolve inherited timelines: if no class-level record exists, fall back to course template metadata.
  - Clone course templates on demand when instructors request custom variants (`scope = CLASS_CLONE`).
- **AssignmentService / QuizService Adjustments**
  - Accept optional `scope`, `class_definition_uuid`, and `source_*_uuid` parameters.
  - Restrict duplicate titles within a class to avoid student confusion.
  - Emit domain events consumed by notifications.
- **Notification Coordination**
  - Subscribe to schedule-change events and schedule reminders relative to `visible_at` / `due_at`.
  - Cancel reminders when deadlines shift or a schedule entry is deleted.

### Business Rules
- Only the instructor assigned to the class (or organisation admins) can manipulate schedules or create class-level clones.
- Deadlines (`due_at`) must be strictly after `visible_at`; both must be in UTC.
- Updates to override fields must honour constraints defined on the original template.

### API Surface (Instructor-Facing)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/classes/{classUuid}/assignments` | GET/POST | List scheduled assignments for the class or create a new schedule entry (inherit or clone). |
| `/api/classes/{classUuid}/assignments/{scheduleUuid}` | PATCH/DELETE | Update deadlines/overrides or deactivate an assignment for the class. |
| `/api/classes/{classUuid}/quizzes` | GET/POST | Same semantics as assignments for quizzes. |
| `/api/classes/{classUuid}/quizzes/{scheduleUuid}` | PATCH/DELETE | Update or deactivate quiz schedules. |
| `/api/classes/{classUuid}/assessments/import` | POST | Bulk-import course templates into the class schedule with default timings. |

All routes are secured under existing instructor roles with organisation scoping checks.

## Implementation Roadmap

### Phase 1 – Persistence & Domain
- Create Flyway migrations for assignment/quiz metadata and class schedule tables.
- Introduce enum types plus attribute converters and validation helpers.
- Add JPA entities, repositories, and DTO/factory classes for `ClassAssignmentSchedule` and `ClassQuizSchedule`.

### Phase 2 – Service Layer
- Build `ClassAssessmentScheduleService` handling instructor overrides, clone workflows, and business-rule validation.
- Update existing assignment/quiz services to respect scope metadata and expose clone helpers.

### Phase 3 – API & Security
- Add instructor-facing controllers under the `classes` module exposing assessment-schedule endpoints.
- Wire security checks ensuring only authorised trainers (or org admins) mutate class schedules.
- Integrate request/response DTOs with validation annotations for deadlines and overrides.

### Phase 4 – Integration & Notifications
- Emit schedule change events and update the notification module to dispatch due/visibility reminders.
- Adjust analytics/reporting queries to pull from class schedules when present.

### Phase 5 – Testing & Documentation
- Add JUnit/Testcontainers coverage for new repositories and service rules, focusing on inheritance vs override scenarios.
- Extend API integration tests to cover new instructor endpoints.
- Update `docs/` with instructor workflow guides and release notes for configuration changes.
