-- 202507021540__create_difficulty_levels_table.sql
-- Create difficulty levels table (Prep, Beginner, Intermediate, Advanced)

CREATE TABLE course_difficulty_levels
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name         VARCHAR(50)              NOT NULL UNIQUE,
    level_order  INTEGER                  NOT NULL UNIQUE,
    description  TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by   VARCHAR(255)             NOT NULL,
    updated_by   VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX idx_course_difficulty_levels_uuid ON course_difficulty_levels (uuid);
CREATE INDEX idx_course_difficulty_levels_order ON course_difficulty_levels (level_order);
CREATE INDEX idx_course_difficulty_levels_name ON course_difficulty_levels (name);
CREATE INDEX idx_course_difficulty_levels_created_date ON course_difficulty_levels (created_date);