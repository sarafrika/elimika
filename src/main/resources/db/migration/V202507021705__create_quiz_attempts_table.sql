-- 202507021705__create_quiz_attempts_table.sql
-- Create quiz attempts table for tracking student quiz submissions

CREATE TABLE quiz_attempts
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    enrollment_uuid    UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    quiz_uuid          UUID                     NOT NULL REFERENCES quizzes (uuid),
    attempt_number     INTEGER                  NOT NULL,
    started_at         TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    submitted_at       TIMESTAMP WITH TIME ZONE,
    time_taken_minutes INTEGER,
    score              DECIMAL(5, 2),
    max_score          DECIMAL(5, 2),
    percentage         DECIMAL(5, 2),
    is_passed          BOOLEAN,
    status             VARCHAR(20)              NOT NULL        DEFAULT 'in_progress' CHECK (status IN ('in_progress', 'submitted', 'graded')),
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255),
    UNIQUE (enrollment_uuid, quiz_uuid, attempt_number)
);

-- Create performance indexes
CREATE INDEX idx_quiz_attempts_uuid ON quiz_attempts (uuid);
CREATE INDEX idx_quiz_attempts_enrollment_uuid ON quiz_attempts (enrollment_uuid);
CREATE INDEX idx_quiz_attempts_quiz_uuid ON quiz_attempts (quiz_uuid);
CREATE INDEX idx_quiz_attempts_status ON quiz_attempts (status);
CREATE INDEX idx_quiz_attempts_created_date ON quiz_attempts (created_date);