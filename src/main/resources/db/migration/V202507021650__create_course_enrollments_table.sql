-- 202507021650__create_course_enrollments_table.sql
-- Create course enrollments table for student registration and progress

CREATE TABLE course_enrollments
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid        UUID                     NOT NULL REFERENCES students (uuid),
    course_uuid         UUID                     NOT NULL REFERENCES courses (uuid),
    enrollment_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    completion_date     TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(20)              NOT NULL        DEFAULT 'active' CHECK (status IN ('active', 'completed', 'dropped', 'suspended')),
    progress_percentage DECIMAL(5, 2)                            DEFAULT 0.00,
    final_grade         DECIMAL(5, 2),
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    UNIQUE (student_uuid, course_uuid)
);

-- Create performance indexes
CREATE INDEX idx_course_enrollments_uuid ON course_enrollments (uuid);
CREATE INDEX idx_course_enrollments_student_uuid ON course_enrollments (student_uuid);
CREATE INDEX idx_course_enrollments_course_uuid ON course_enrollments (course_uuid);
CREATE INDEX idx_course_enrollments_status ON course_enrollments (status);
CREATE INDEX idx_course_enrollments_enrollment_date ON course_enrollments (enrollment_date);
CREATE INDEX idx_course_enrollments_created_date ON course_enrollments (created_date);