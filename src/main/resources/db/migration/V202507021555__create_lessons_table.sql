-- 202507021555__create_lessons_table.sql
-- Create lessons table for course content organization

CREATE TABLE lessons
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid         UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    lesson_number       INTEGER                  NOT NULL,
    title               VARCHAR(255)             NOT NULL,
    duration_hours      INTEGER                  NOT NULL        DEFAULT 0,
    duration_minutes    INTEGER                  NOT NULL        DEFAULT 0,
    description         TEXT,
    learning_objectives TEXT,
    is_published        BOOLEAN                                  DEFAULT false,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    UNIQUE (course_uuid, lesson_number)
);

-- Create performance indexes
CREATE INDEX idx_lessons_uuid ON lessons (uuid);
CREATE INDEX idx_lessons_course_uuid ON lessons (course_uuid);
CREATE INDEX idx_lessons_lesson_number ON lessons (lesson_number);
CREATE INDEX idx_lessons_is_published ON lessons (is_published);
CREATE INDEX idx_lessons_created_date ON lessons (created_date);