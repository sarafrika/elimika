-- 202507021755__create_certificates_table.sql
-- Create certificates table for issued completion certificates

CREATE TABLE certificates
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    certificate_number VARCHAR(100) UNIQUE      NOT NULL,
    student_uuid       UUID                     NOT NULL REFERENCES users (uuid),
    course_uuid        UUID REFERENCES courses (uuid),
    program_uuid       UUID REFERENCES training_programs (uuid),
    template_uuid      UUID                     NOT NULL REFERENCES certificate_templates (uuid),
    issued_date        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    completion_date    TIMESTAMP WITH TIME ZONE,
    final_grade        DECIMAL(5, 2),
    certificate_url    VARCHAR(500), -- URL to generated certificate PDF
    is_valid           BOOLEAN                                  DEFAULT true,
    revoked_at         TIMESTAMP WITH TIME ZONE,
    revoked_reason     TEXT,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255),
    CONSTRAINT check_course_or_program CHECK (
        (course_uuid IS NOT NULL AND program_uuid IS NULL) OR
        (course_uuid IS NULL AND program_uuid IS NOT NULL)
        )
);

-- Create performance indexes
CREATE INDEX idx_certificates_uuid ON certificates (uuid);
CREATE INDEX idx_certificates_student_uuid ON certificates (student_uuid);
CREATE INDEX idx_certificates_course_uuid ON certificates (course_uuid);
CREATE INDEX idx_certificates_program_uuid ON certificates (program_uuid);
CREATE INDEX idx_certificates_certificate_number ON certificates (certificate_number);
CREATE INDEX idx_certificates_is_valid ON certificates (is_valid);
CREATE INDEX idx_certificates_created_date ON certificates (created_date);