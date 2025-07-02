-- 202507021645__create_assignments_table.sql
-- Create assignments table for lesson practical work

CREATE TABLE assignments
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_uuid      UUID                     NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    title            VARCHAR(255)             NOT NULL,
    description      TEXT,
    instructions     TEXT,
    due_date         TIMESTAMP WITH TIME ZONE,
    max_points       DECIMAL(5, 2)            NOT NULL        DEFAULT 100.00,
    rubric_uuid      UUID REFERENCES assessment_rubrics (uuid),
    submission_types TEXT[], -- Array: file_upload, text_entry, url, etc.
    is_published     BOOLEAN                                  DEFAULT false,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_assignments_uuid ON assignments (uuid);
CREATE INDEX idx_assignments_lesson_uuid ON assignments (lesson_uuid);
CREATE INDEX idx_assignments_rubric_uuid ON assignments (rubric_uuid);
CREATE INDEX idx_assignments_due_date ON assignments (due_date);
CREATE INDEX idx_assignments_is_published ON assignments (is_published);
CREATE INDEX idx_assignments_created_date ON assignments (created_date);