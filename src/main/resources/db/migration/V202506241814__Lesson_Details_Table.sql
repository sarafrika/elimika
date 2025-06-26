-- V202506241814__create_lessons_details_table.sql

-- Create the ENUM type for lesson_type if it doesn't exist
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lesson_content_type') THEN
            CREATE TYPE lesson_content_type AS ENUM (
                'TEXT',
                'VIDEO',
                'PDF',
                'YOUTUBE_LINK'
                );
        END IF;
    END
$$;

-- Create the Lessons table
CREATE TABLE IF NOT EXISTS lessons
(
    id                         BIGSERIAL PRIMARY KEY,
    uuid                       UUID UNIQUE         NOT NULL DEFAULT gen_random_uuid(),
    lesson_no                  INTEGER             NOT NULL,
    course_uuid                UUID                NOT NULL,
    lesson_name                VARCHAR(255)        NOT NULL,
    lesson_description         TEXT,
    lesson_type                lesson_content_type NOT NULL,
    estimated_duration_minutes INTEGER,
    created_date               TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date               TIMESTAMPTZ                  DEFAULT CURRENT_TIMESTAMP,
    created_by                 VARCHAR(255)        NOT NULL,
    updated_by                 VARCHAR(255),

    -- Foreign key constraint
    CONSTRAINT fk_lesson_course_uuid FOREIGN KEY (course_uuid) REFERENCES courses (uuid) ON DELETE CASCADE,

    -- Unique constraint for lesson number within a course
    CONSTRAINT uk_lesson_no_course_uuid UNIQUE (course_uuid, lesson_no)
);

-- Add indexes for frequently queried columns
CREATE INDEX IF NOT EXISTS idx_lessons_course_uuid ON lessons (course_uuid);
CREATE INDEX IF NOT EXISTS idx_lessons_lesson_no ON lessons (lesson_no);
CREATE INDEX IF NOT EXISTS idx_lessons_lesson_type ON lessons (lesson_type);
CREATE INDEX IF NOT EXISTS idx_lessons_uuid ON lessons (uuid);
CREATE INDEX IF NOT EXISTS idx_lessons_course_lesson_no ON lessons (course_uuid, lesson_no);