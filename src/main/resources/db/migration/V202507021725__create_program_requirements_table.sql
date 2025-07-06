-- 202507021725__create_program_requirements_table.sql
-- Create program requirements table for training program prerequisites

CREATE TABLE program_requirements
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    program_uuid     UUID                     NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    requirement_type VARCHAR(20)              NOT NULL CHECK (requirement_type IN ('student', 'training_center', 'instructor')),
    requirement_text TEXT                     NOT NULL,
    is_mandatory     BOOLEAN                                  DEFAULT true,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_program_requirements_uuid ON program_requirements (uuid);
CREATE INDEX idx_program_requirements_program_uuid ON program_requirements (program_uuid);
CREATE INDEX idx_program_requirements_type ON program_requirements (requirement_type);
CREATE INDEX idx_program_requirements_created_date ON program_requirements (created_date);