-- 202507021545__create_courses_table.sql
-- Create main courses table

CREATE TABLE courses
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name             VARCHAR(255)             NOT NULL,
    instructor_uuid  UUID                     NOT NULL REFERENCES instructors (uuid),
    category_uuid    UUID REFERENCES course_categories (uuid),
    difficulty_uuid  UUID REFERENCES course_difficulty_levels (uuid),
    description      TEXT,
    objectives       TEXT,
    prerequisites    TEXT,
    duration_hours   INTEGER                  NOT NULL        DEFAULT 0,
    duration_minutes INTEGER                  NOT NULL        DEFAULT 0,
    class_limit      INTEGER,
    price            DECIMAL(10, 2),
    age_lower_limit  INTEGER CHECK (age_lower_limit >= 1 AND age_lower_limit <= 120),
    age_upper_limit  INTEGER CHECK (age_upper_limit >= 1 AND age_upper_limit <= 120),
    thumbnail_url    VARCHAR(500),
    intro_video_url  VARCHAR(500),
    banner_url       VARCHAR(500),
    status           VARCHAR(20)              NOT NULL        DEFAULT 'draft' CHECK (status IN ('draft', 'in_review', 'published', 'archived')),
    active           BOOLEAN                  NOT NULL        DEFAULT false,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255),
    CONSTRAINT check_active_only_if_published CHECK (active = false OR (active = true AND status = 'published')),
    CONSTRAINT check_age_limits CHECK (age_lower_limit IS NULL OR age_upper_limit IS NULL OR
                                       age_lower_limit <= age_upper_limit)
);

-- Create performance indexes
CREATE INDEX idx_courses_uuid ON courses (uuid);
CREATE INDEX idx_courses_instructor_uuid ON courses (instructor_uuid);
CREATE INDEX idx_courses_category_uuid ON courses (category_uuid);
CREATE INDEX idx_courses_difficulty_uuid ON courses (difficulty_uuid);
CREATE INDEX idx_courses_status ON courses (status);
CREATE INDEX idx_courses_active ON courses (active);
CREATE INDEX idx_courses_created_date ON courses (created_date);
CREATE INDEX idx_courses_name ON courses (name);
CREATE INDEX idx_courses_age_lower_limit ON courses (age_lower_limit);
CREATE INDEX idx_courses_age_upper_limit ON courses (age_upper_limit);