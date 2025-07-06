-- 202507021730__create_program_courses_table.sql
-- Create program courses table for linking courses to training programs

CREATE TABLE program_courses
(
    id                       BIGSERIAL PRIMARY KEY,
    uuid                     UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    program_uuid             UUID                     NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    course_uuid              UUID                     NOT NULL REFERENCES courses (uuid),
    sequence_order           INTEGER                  NOT NULL,
    is_required              BOOLEAN                                  DEFAULT true,
    prerequisite_course_uuid UUID REFERENCES courses (uuid),
    created_date             TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date             TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by               VARCHAR(255)             NOT NULL,
    updated_by               VARCHAR(255),
    UNIQUE (program_uuid, course_uuid),
    UNIQUE (program_uuid, sequence_order)
);

-- Create performance indexes
CREATE INDEX idx_program_courses_uuid ON program_courses (uuid);
CREATE INDEX idx_program_courses_program_uuid ON program_courses (program_uuid);
CREATE INDEX idx_program_courses_course_uuid ON program_courses (course_uuid);
CREATE INDEX idx_program_courses_sequence_order ON program_courses (sequence_order);
CREATE INDEX idx_program_courses_prerequisite_uuid ON program_courses (prerequisite_course_uuid);
CREATE INDEX idx_program_courses_created_date ON program_courses (created_date);