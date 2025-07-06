-- 202507021615__create_assessment_rubrics_table.sql
-- Create assessment rubrics table for evaluation criteria

CREATE TABLE assessment_rubrics
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title           VARCHAR(255)             NOT NULL,
    description     TEXT,
    course_uuid     UUID REFERENCES courses (uuid),
    rubric_type     VARCHAR(50)              NOT NULL, -- Assignment, Exam, Attendance, Quiz, etc.
    instructor_uuid UUID                     NOT NULL REFERENCES instructors (uuid),
    is_public       BOOLEAN                                  DEFAULT false,
    is_active       BOOLEAN                                  DEFAULT true,
    status          VARCHAR(20)              NOT NULL        DEFAULT 'draft' CHECK (status IN ('draft', 'in_review', 'published', 'archived')),
    created_date    TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date    TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by      VARCHAR(255)             NOT NULL,
    updated_by      VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_assessment_rubrics_uuid ON assessment_rubrics (uuid);
CREATE INDEX idx_assessment_rubrics_course_uuid ON assessment_rubrics (course_uuid);
CREATE INDEX idx_assessment_rubrics_author_uuid ON assessment_rubrics (instructor_uuid);
CREATE INDEX idx_assessment_rubrics_type ON assessment_rubrics (rubric_type);
CREATE INDEX idx_assessment_rubrics_is_public ON assessment_rubrics (is_public);
CREATE INDEX idx_assessment_rubrics_created_date ON assessment_rubrics (created_date);