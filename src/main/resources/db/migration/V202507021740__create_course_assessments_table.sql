-- 202507021740__create_course_assessments_table.sql
-- Create course assessments table for overall course evaluation (Attendance, Assignments, Exams)

CREATE TABLE course_assessments
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid       UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    assessment_type   VARCHAR(50)              NOT NULL,                     -- Attendance, Class Assignment, Quiz, Exam
    title             VARCHAR(255)             NOT NULL,
    description       TEXT,
    weight_percentage DECIMAL(5, 2)            NOT NULL        DEFAULT 0.00, -- How much this assessment contributes to final grade
    rubric_uuid       UUID REFERENCES assessment_rubrics (uuid),
    is_required       BOOLEAN                                  DEFAULT true,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date      TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_course_assessments_uuid ON course_assessments (uuid);
CREATE INDEX idx_course_assessments_course_uuid ON course_assessments (course_uuid);
CREATE INDEX idx_course_assessments_type ON course_assessments (assessment_type);
CREATE INDEX idx_course_assessments_rubric_uuid ON course_assessments (rubric_uuid);
CREATE INDEX idx_course_assessments_created_date ON course_assessments (created_date);