-- Instructor progress remarks on a learner's programme; append-only so earlier remarks stay visible.
CREATE TABLE IF NOT EXISTS program_enrollment_remarks
(
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    program_enrollment_uuid UUID         NOT NULL REFERENCES program_enrollments (uuid) ON DELETE CASCADE,
    strengths               TEXT,
    improvements            TEXT,
    recommendations         TEXT,
    next_review_date        DATE,
    author_user_uuid        UUID REFERENCES users (uuid) ON DELETE SET NULL,
    created_date            TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by              VARCHAR(255) NOT NULL,
    updated_date            TIMESTAMP,
    updated_by              VARCHAR(255),
    CONSTRAINT chk_program_enrollment_remarks_text CHECK (
        COALESCE(LENGTH(TRIM(strengths)), 0) + COALESCE(LENGTH(TRIM(improvements)), 0)
            + COALESCE(LENGTH(TRIM(recommendations)), 0) > 0)
);

CREATE INDEX IF NOT EXISTS idx_program_enrollment_remarks_enrollment
    ON program_enrollment_remarks (program_enrollment_uuid, created_date DESC);
