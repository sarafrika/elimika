-- V202506241700__Course_Details_Table.sql

-- Create the ENUM type for difficulty if it doesn't exist
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'difficulty_level') THEN
            CREATE TYPE difficulty_level AS ENUM (
                'BEGINNER',
                'INTERMEDIATE',
                'ADVANCED',
                'EXPERT'
                );
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'status_type') THEN
            CREATE TYPE status_type AS ENUM (
                'ACTIVE',
                'INACTIVE',
                'ARCHIVED',
                'DRAFT'
                );
        END IF;
    END
$$;

-- Create the Course table
CREATE TABLE IF NOT EXISTS courses
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID UNIQUE         NOT NULL DEFAULT gen_random_uuid(),
    course_code        VARCHAR(255) UNIQUE NOT NULL,
    course_name        VARCHAR(255)        NOT NULL,
    course_thumbnail   VARCHAR(255),
    course_description TEXT,
    course_status      status_type                  DEFAULT 'DRAFT',
    initial_price      NUMERIC(19, 2),
    current_price      NUMERIC(19, 2),
    access_start_date  TIMESTAMPTZ,
    class_limit        INTEGER,
    age_upper_limit    INTEGER,
    age_lower_limit    INTEGER,
    difficulty         difficulty_level,
    course_objectives  TEXT,
    created_date       TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date       TIMESTAMPTZ                  DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(255)        NOT NULL,
    updated_by         VARCHAR(255)
);

-- Add indexes for frequently queried columns
CREATE INDEX IF NOT EXISTS idx_courses_course_code ON courses (course_code);
CREATE INDEX IF NOT EXISTS idx_courses_access_start_date ON courses (access_start_date);
CREATE INDEX IF NOT EXISTS idx_courses_status ON courses (course_status);
CREATE INDEX IF NOT EXISTS idx_courses_difficulty ON courses (difficulty);
CREATE INDEX IF NOT EXISTS idx_courses_uuid ON courses (uuid);