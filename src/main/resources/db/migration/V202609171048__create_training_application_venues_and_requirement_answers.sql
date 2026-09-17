-- What an applicant offers besides rates: the venues an organisation would train in, and whether the
-- applicant has each training requirement (and if not, whether they would lease or hire it).

CREATE TABLE training_application_venues
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_type VARCHAR(16)  NOT NULL,
    application_uuid UUID         NOT NULL,
    resource_uuid    UUID         NOT NULL,
    created_date     TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by       VARCHAR(255) NOT NULL,
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_training_application_venues_application_type
        CHECK (UPPER(application_type) IN ('COURSE', 'PROGRAM')),
    CONSTRAINT uq_training_application_venue UNIQUE (application_type, application_uuid, resource_uuid)
);

CREATE INDEX idx_training_application_venues_resource
    ON training_application_venues (resource_uuid);

CREATE TABLE training_application_requirement_answers
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_type VARCHAR(16)  NOT NULL,
    application_uuid UUID         NOT NULL,
    requirement_uuid UUID         NOT NULL,
    has_it           BOOLEAN      NOT NULL,
    acquisition      VARCHAR(16),
    created_date     TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by       VARCHAR(255) NOT NULL,
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_training_application_requirement_answers_application_type
        CHECK (UPPER(application_type) IN ('COURSE', 'PROGRAM')),
    CONSTRAINT chk_training_application_requirement_answers_acquisition
        CHECK (acquisition IS NULL OR UPPER(acquisition) IN ('LEASE', 'HIRE')),
    -- Someone who lacks a requirement has to say how they would obtain it.
    CONSTRAINT chk_training_application_requirement_answers_acquisition_when_missing
        CHECK (has_it OR acquisition IS NOT NULL),
    CONSTRAINT uq_training_application_requirement_answer UNIQUE (application_type, application_uuid, requirement_uuid)
);
