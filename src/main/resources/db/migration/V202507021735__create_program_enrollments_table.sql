-- 202507021735__create_program_enrollments_table.sql
-- Create program enrollments table for student registration in training programs

CREATE TABLE program_enrollments
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid        UUID                     NOT NULL REFERENCES students (uuid),
    program_uuid        UUID                     NOT NULL REFERENCES training_programs (uuid),
    enrollment_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    completion_date     TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(20)              NOT NULL        DEFAULT 'active' CHECK (status IN ('active', 'completed', 'dropped', 'suspended')),
    progress_percentage DECIMAL(5, 2)                            DEFAULT 0.00,
    final_grade         DECIMAL(5, 2),
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    UNIQUE (student_uuid, program_uuid)
);

-- Create performance indexes
CREATE INDEX idx_program_enrollments_uuid ON program_enrollments (uuid);
CREATE INDEX idx_program_enrollments_student_uuid ON program_enrollments (student_uuid);
CREATE INDEX idx_program_enrollments_program_uuid ON program_enrollments (program_uuid);
CREATE INDEX idx_program_enrollments_status ON program_enrollments (status);
CREATE INDEX idx_program_enrollments_created_date ON program_enrollments (created_date);