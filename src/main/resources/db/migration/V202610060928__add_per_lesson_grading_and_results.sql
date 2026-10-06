-- Per-lesson grading: a component can be graded lesson by lesson, each cell a line item tied to a
-- lesson. A session can name the lesson it teaches. Enrolments record the pass/fail result.
ALTER TABLE course_assessments
    ADD COLUMN IF NOT EXISTS per_lesson BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE course_assessment_line_items
    ADD COLUMN IF NOT EXISTS lesson_uuid UUID REFERENCES lessons (uuid) ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_course_assessment_line_items_lesson_cell
    ON course_assessment_line_items (course_assessment_uuid, lesson_uuid)
    WHERE lesson_uuid IS NOT NULL;

ALTER TABLE scheduled_instances
    ADD COLUMN IF NOT EXISTS lesson_uuid UUID REFERENCES lessons (uuid) ON DELETE SET NULL;

ALTER TABLE course_enrollments
    ADD COLUMN IF NOT EXISTS result_status     VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN IF NOT EXISTS result_decided_at TIMESTAMP;

ALTER TABLE course_enrollments
    DROP CONSTRAINT IF EXISTS chk_course_enrollments_result_status;
ALTER TABLE course_enrollments
    ADD CONSTRAINT chk_course_enrollments_result_status
        CHECK (UPPER(result_status) IN ('IN_PROGRESS', 'PASSED', 'FAILED'));

ALTER TABLE program_enrollments
    ADD COLUMN IF NOT EXISTS result_status     VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN IF NOT EXISTS result_decided_at TIMESTAMP;

ALTER TABLE program_enrollments
    DROP CONSTRAINT IF EXISTS chk_program_enrollments_result_status;
ALTER TABLE program_enrollments
    ADD CONSTRAINT chk_program_enrollments_result_status
        CHECK (UPPER(result_status) IN ('IN_PROGRESS', 'PASSED', 'FAILED'));
