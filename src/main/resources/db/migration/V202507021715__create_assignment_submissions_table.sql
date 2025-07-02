-- 202507021715__create_assignment_submissions_table.sql
-- Create assignment submissions table for student assignment uploads

CREATE TABLE assignment_submissions
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    enrollment_uuid     UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    assignment_uuid     UUID                     NOT NULL REFERENCES assignments (uuid),
    submission_text     TEXT,
    file_urls           TEXT[], -- Array of submitted file URLs
    submitted_at        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    status              VARCHAR(20)              NOT NULL        DEFAULT 'submitted' CHECK (status IN ('draft', 'submitted', 'graded', 'returned')),
    score               DECIMAL(5, 2),
    max_score           DECIMAL(5, 2),
    percentage          DECIMAL(5, 2),
    instructor_comments TEXT,
    graded_at           TIMESTAMP WITH TIME ZONE,
    graded_by_uuid      UUID REFERENCES users (uuid),
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    UNIQUE (enrollment_uuid, assignment_uuid)
);

-- Create performance indexes
CREATE INDEX idx_assignment_submissions_uuid ON assignment_submissions (uuid);
CREATE INDEX idx_assignment_submissions_enrollment_uuid ON assignment_submissions (enrollment_uuid);
CREATE INDEX idx_assignment_submissions_assignment_uuid ON assignment_submissions (assignment_uuid);
CREATE INDEX idx_assignment_submissions_status ON assignment_submissions (status);
CREATE INDEX idx_assignment_submissions_graded_by_uuid ON assignment_submissions (graded_by_uuid);
CREATE INDEX idx_assignment_submissions_created_date ON assignment_submissions (created_date);