-- Append-only history of every grade given to a learner on a gradebook line item.
-- course_assessment_line_item_scores keeps only the current mark; each save, regrade, quiz
-- attempt or attendance sync also writes one row here, numbered per (line item, enrolment).

CREATE TABLE course_assessment_grade_revisions
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    line_item_uuid  UUID                     NOT NULL REFERENCES course_assessment_line_items (uuid) ON DELETE CASCADE,
    enrollment_uuid UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    revision_number INTEGER                  NOT NULL,
    item_type       VARCHAR(30),
    score           DECIMAL(10, 2),
    max_score       DECIMAL(10, 2),
    percentage      DECIMAL(6, 2),
    comments        TEXT,
    criteria        JSONB,
    graded_at       TIMESTAMP WITH TIME ZONE,
    graded_by_uuid  UUID REFERENCES users (uuid),
    created_date    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date    TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(255)             NOT NULL,
    updated_by      VARCHAR(255),
    CONSTRAINT uq_course_assessment_grade_revision UNIQUE (line_item_uuid, enrollment_uuid, revision_number),
    CONSTRAINT chk_course_assessment_grade_revision_number CHECK (revision_number > 0)
);

CREATE INDEX idx_course_assessment_grade_revisions_enrollment ON course_assessment_grade_revisions (enrollment_uuid);

COMMENT ON COLUMN course_assessment_grade_revisions.criteria IS
    'Per-criterion picks for rubric grades: criterion, level, points and comment. NULL for plain scores.';
