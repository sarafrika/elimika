-- 202507021720__create_training_programs_table.sql
-- Create training programs table for course bundles

CREATE TABLE training_programs
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID               NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title                  VARCHAR(255)       NOT NULL,
    instructor_uuid        UUID               NOT NULL REFERENCES instructors (uuid),
    category_uuid          UUID REFERENCES course_categories (uuid),
    description            TEXT,
    objectives             TEXT,
    prerequisites          TEXT,
    total_duration_hours   INTEGER            NOT NULL        DEFAULT 0,
    total_duration_minutes INTEGER            NOT NULL        DEFAULT 0,
    class_limit            INTEGER,
    price                  DECIMAL(10, 2),
    is_published           BOOLEAN                            DEFAULT false,
    is_active              BOOLEAN                            DEFAULT true,
    status                 VARCHAR(20)        NOT NULL        DEFAULT 'draft' CHECK (status IN ('draft', 'in_review', 'published', 'archived')),
    created_date           TIMESTAMP
                               WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date           TIMESTAMP WITH TIME ZONE           DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by             VARCHAR(255)       NOT NULL,
    updated_by             VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_training_programs_uuid ON training_programs (uuid);
CREATE INDEX idx_training_programs_author_uuid ON training_programs (instructor_uuid);
CREATE INDEX idx_training_programs_category_uuid ON training_programs (category_uuid);
CREATE INDEX idx_training_programs_is_published ON training_programs (is_published);
CREATE INDEX idx_training_programs_created_date ON training_programs (created_date);