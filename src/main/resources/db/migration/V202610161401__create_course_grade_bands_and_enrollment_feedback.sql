-- Per-course grade bands: a label (e.g. Distinction) awarded when the final grade reaches min_percentage.
CREATE TABLE course_grade_bands
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid    UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    name           VARCHAR(50)              NOT NULL,
    min_percentage DECIMAL(5, 2)            NOT NULL,
    display_order  INTEGER                  NOT NULL DEFAULT 0,
    created_date   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date   TIMESTAMP WITH TIME ZONE,
    created_by     VARCHAR(255)             NOT NULL,
    updated_by     VARCHAR(255),
    CONSTRAINT uq_course_grade_bands_name UNIQUE (course_uuid, name),
    CONSTRAINT uq_course_grade_bands_min UNIQUE (course_uuid, min_percentage),
    CONSTRAINT chk_course_grade_bands_min CHECK (min_percentage >= 0 AND min_percentage <= 100)
);

-- Append-only instructor feedback on a learner's enrolment; the newest row is the current comment.
CREATE TABLE course_enrollment_feedback
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    enrollment_uuid  UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    comments         TEXT                     NOT NULL,
    author_user_uuid UUID REFERENCES users (uuid),
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date     TIMESTAMP WITH TIME ZONE,
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_course_enrollment_feedback_comments CHECK (length(trim(comments)) > 0)
);

CREATE INDEX idx_course_enrollment_feedback_enrollment ON course_enrollment_feedback (enrollment_uuid, created_date DESC);
