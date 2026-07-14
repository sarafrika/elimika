-- Which resources a marketplace job (and later the class created from it) wants.
-- Actual time reservations live in resource_bookings (resourcing module).

CREATE TABLE class_marketplace_job_resources
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    job_uuid      UUID         NOT NULL REFERENCES class_marketplace_jobs (uuid) ON DELETE CASCADE,
    resource_uuid UUID         NOT NULL REFERENCES organisation_resources (uuid),
    quantity      INTEGER      NOT NULL DEFAULT 1,
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date  TIMESTAMP,
    created_by    VARCHAR(255) NOT NULL,
    updated_by    VARCHAR(255),
    CONSTRAINT uq_class_marketplace_job_resources UNIQUE (job_uuid, resource_uuid),
    CONSTRAINT chk_class_marketplace_job_resources_quantity_positive
        CHECK (quantity >= 1)
);

CREATE INDEX idx_class_marketplace_job_resources_job_uuid
    ON class_marketplace_job_resources (job_uuid);

CREATE INDEX idx_class_marketplace_job_resources_resource_uuid
    ON class_marketplace_job_resources (resource_uuid);

CREATE TABLE class_definition_resources
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    class_definition_uuid UUID         NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    resource_uuid         UUID         NOT NULL REFERENCES organisation_resources (uuid),
    quantity              INTEGER      NOT NULL DEFAULT 1,
    created_date          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date          TIMESTAMP,
    created_by            VARCHAR(255) NOT NULL,
    updated_by            VARCHAR(255),
    CONSTRAINT uq_class_definition_resources UNIQUE (class_definition_uuid, resource_uuid),
    CONSTRAINT chk_class_definition_resources_quantity_positive
        CHECK (quantity >= 1)
);

CREATE INDEX idx_class_definition_resources_class_definition_uuid
    ON class_definition_resources (class_definition_uuid);

CREATE INDEX idx_class_definition_resources_resource_uuid
    ON class_definition_resources (resource_uuid);

COMMENT ON TABLE class_marketplace_job_resources IS 'Resources a marketplace job requests for its sessions; drives recruitment HOLD bookings';
COMMENT ON TABLE class_definition_resources IS 'Resources a class uses for its sessions; copied from the source job at assignment';
