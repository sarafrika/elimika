-- 202601311228__create_program_training_applications_table.sql
-- Adds training program applications to manage instructor and organisation approvals

CREATE TABLE program_training_applications
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                   NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    program_uuid        UUID                   NOT NULL REFERENCES training_programs (uuid),
    applicant_type      VARCHAR(32)            NOT NULL,
    applicant_uuid      UUID                   NOT NULL,
    rate_currency       VARCHAR(3)             NOT NULL DEFAULT 'KES',
    private_online_rate NUMERIC(12, 4)         NOT NULL,
    private_inperson_rate NUMERIC(12, 4)       NOT NULL,
    group_online_rate   NUMERIC(12, 4)         NOT NULL,
    group_inperson_rate NUMERIC(12, 4)         NOT NULL,
    status              VARCHAR(32)            NOT NULL,
    application_notes   TEXT,
    review_notes        TEXT,
    reviewed_by         VARCHAR(255),
    reviewed_at         TIMESTAMP,
    created_date        TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    created_by          VARCHAR(255)           NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_program_training_applications_applicant_type
        CHECK (UPPER(applicant_type) IN ('INSTRUCTOR', 'ORGANISATION')),
    CONSTRAINT chk_program_training_applications_status
        CHECK (UPPER(status) IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED')),
    CONSTRAINT chk_program_training_applications_currency_length
        CHECK (char_length(rate_currency) = 3),
    CONSTRAINT chk_program_training_rate_card_modalities_non_negative
        CHECK (
            private_online_rate >= 0
            AND private_inperson_rate >= 0
            AND group_online_rate >= 0
            AND group_inperson_rate >= 0
        ),
    CONSTRAINT uq_program_training_application UNIQUE (program_uuid, applicant_type, applicant_uuid),
    CONSTRAINT fk_program_training_application_currency FOREIGN KEY (rate_currency) REFERENCES currencies (code)
);

CREATE INDEX idx_program_training_applications_program_uuid
    ON program_training_applications (program_uuid);

CREATE INDEX idx_program_training_applications_status
    ON program_training_applications (status);

CREATE INDEX idx_program_training_applications_applicant
    ON program_training_applications (applicant_type, applicant_uuid);
