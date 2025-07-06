-- 202507021550__create_course_requirements_table.sql
-- Create course requirements table (student, training center, instructor requirements)

CREATE TABLE course_requirements
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid      UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    requirement_type VARCHAR(20)              NOT NULL CHECK (requirement_type IN ('student', 'training_center', 'instructor')),
    requirement_text TEXT                     NOT NULL,
    is_mandatory     BOOLEAN                                  DEFAULT true,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255)
);

-- Create indexes
CREATE INDEX idx_course_requirements_uuid ON course_requirements (uuid);
CREATE INDEX idx_course_requirements_course_uuid ON course_requirements (course_uuid);
CREATE INDEX idx_course_requirements_type ON course_requirements (requirement_type);
CREATE INDEX idx_course_requirements_created_date ON course_requirements (created_date);