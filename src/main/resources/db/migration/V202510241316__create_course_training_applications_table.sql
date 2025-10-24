-- 202510241316__create_course_training_applications_table.sql
-- Adds course training applications to manage instructor and organisation approvals

CREATE TABLE course_training_applications
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                   NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    course_uuid       UUID                   NOT NULL REFERENCES courses (uuid),
    applicant_type    VARCHAR(32)            NOT NULL,
    applicant_uuid    UUID                   NOT NULL,
    status            VARCHAR(32)            NOT NULL,
    application_notes TEXT,
    review_notes      TEXT,
    reviewed_by       VARCHAR(255),
    reviewed_at       TIMESTAMP,
    created_date      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP,
    created_by        VARCHAR(255)           NOT NULL,
    updated_by        VARCHAR(255),
    CONSTRAINT chk_course_training_applications_applicant_type
        CHECK (applicant_type IN ('INSTRUCTOR', 'ORGANISATION')),
    CONSTRAINT chk_course_training_applications_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED')),
    CONSTRAINT uq_course_training_application UNIQUE (course_uuid, applicant_type, applicant_uuid)
);

CREATE INDEX idx_course_training_applications_course_uuid
    ON course_training_applications (course_uuid);

CREATE INDEX idx_course_training_applications_status
    ON course_training_applications (status);

CREATE INDEX idx_course_training_applications_applicant
    ON course_training_applications (applicant_type, applicant_uuid);
