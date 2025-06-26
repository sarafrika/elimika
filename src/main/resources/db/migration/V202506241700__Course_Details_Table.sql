-- V202506241700__Course_Details_Table.sql

-- Create the ENUM type for difficulty if it doesn't exist
DO $$
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

DO $$
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
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    course_code VARCHAR(255) UNIQUE NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    course_thumbnail VARCHAR(255),
    course_description TEXT,
    course_status status_type DEFAULT 'DRAFT';
    initial_price NUMERIC(19, 2) NOT NULL,
    current_price NUMERIC(19, 2) NOT NULL,
    access_start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    class_limit INTEGER NOT NULL,
    age_upper_limit INTEGER,
    age_lower_limit INTEGER,
    difficulty difficulty_level NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255)
);

-- Add indexes for frequently queried columns if needed (optional, but good practice)
CREATE INDEX IF NOT EXISTS idx_course_code ON course (course_code);
CREATE INDEX IF NOT EXISTS idx_access_start_date ON course (access_start_date);
