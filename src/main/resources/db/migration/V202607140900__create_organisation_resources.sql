-- Bookable physical resources owned by organisations (venues, equipment pools)
-- with Google-Calendar-style availability rules and time-slot bookings.

CREATE TABLE organisation_resources
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    organisation_uuid  UUID         NOT NULL REFERENCES organisation (uuid),
    branch_uuid        UUID         REFERENCES training_branches (uuid),
    resource_type      VARCHAR(32)  NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    seat_capacity      INTEGER,
    total_quantity     INTEGER,
    location_name      VARCHAR(255),
    location_latitude  NUMERIC(9, 6),
    location_longitude NUMERIC(9, 6),
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date       TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255),
    CONSTRAINT chk_organisation_resources_type
        CHECK (resource_type IN ('VENUE', 'EQUIPMENT_POOL')),
    CONSTRAINT chk_organisation_resources_venue_shape
        CHECK (resource_type <> 'VENUE' OR (seat_capacity >= 1 AND total_quantity IS NULL)),
    CONSTRAINT chk_organisation_resources_equipment_shape
        CHECK (resource_type <> 'EQUIPMENT_POOL' OR (total_quantity >= 1 AND seat_capacity IS NULL)),
    CONSTRAINT uq_organisation_resources_org_name UNIQUE (organisation_uuid, name)
);

CREATE INDEX idx_organisation_resources_organisation_uuid
    ON organisation_resources (organisation_uuid);

CREATE INDEX idx_organisation_resources_branch_uuid
    ON organisation_resources (branch_uuid)
    WHERE branch_uuid IS NOT NULL;

CREATE TABLE resource_availability_rules
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    resource_uuid        UUID         NOT NULL REFERENCES organisation_resources (uuid) ON DELETE CASCADE,
    rule_type            VARCHAR(16)  NOT NULL,
    days_of_week         VARCHAR(128),
    start_time           TIME,
    end_time             TIME,
    specific_start       TIMESTAMP,
    specific_end         TIMESTAMP,
    effective_start_date DATE,
    effective_end_date   DATE,
    notes                VARCHAR(500),
    created_date         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date         TIMESTAMP,
    created_by           VARCHAR(255) NOT NULL,
    updated_by           VARCHAR(255),
    CONSTRAINT chk_resource_availability_rules_type
        CHECK (rule_type IN ('OPEN_HOURS', 'BLACKOUT')),
    CONSTRAINT chk_resource_availability_rules_shape
        CHECK (
            (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time
                AND specific_start IS NULL AND specific_end IS NULL)
            OR (specific_start IS NOT NULL AND specific_end IS NOT NULL AND specific_start < specific_end
                AND start_time IS NULL AND end_time IS NULL)
        ),
    CONSTRAINT chk_resource_availability_rules_specific_blackout_only
        CHECK (specific_start IS NULL OR rule_type = 'BLACKOUT'),
    CONSTRAINT chk_resource_availability_rules_effective_period_valid
        CHECK (
            effective_start_date IS NULL
            OR effective_end_date IS NULL
            OR effective_start_date <= effective_end_date
        )
);

CREATE INDEX idx_resource_availability_rules_resource_uuid
    ON resource_availability_rules (resource_uuid);

CREATE TABLE resource_bookings
(
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    resource_uuid           UUID         NOT NULL REFERENCES organisation_resources (uuid),
    organisation_uuid       UUID         NOT NULL REFERENCES organisation (uuid),
    status                  VARCHAR(16)  NOT NULL,
    quantity                INTEGER      NOT NULL DEFAULT 1,
    start_time              TIMESTAMP    NOT NULL,
    end_time                TIMESTAMP    NOT NULL,
    source_type             VARCHAR(32)  NOT NULL,
    job_uuid                UUID         REFERENCES class_marketplace_jobs (uuid),
    class_definition_uuid   UUID         REFERENCES class_definitions (uuid),
    scheduled_instance_uuid UUID         REFERENCES scheduled_instances (uuid),
    released_at             TIMESTAMP,
    release_reason          VARCHAR(500),
    created_date            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date            TIMESTAMP,
    created_by              VARCHAR(255) NOT NULL,
    updated_by              VARCHAR(255),
    CONSTRAINT chk_resource_bookings_status
        CHECK (status IN ('HOLD', 'CONFIRMED', 'RELEASED', 'CANCELLED')),
    CONSTRAINT chk_resource_bookings_source_type
        CHECK (source_type IN ('MARKETPLACE_JOB', 'CLASS_DEFINITION', 'MANUAL')),
    CONSTRAINT chk_resource_bookings_quantity_positive
        CHECK (quantity >= 1),
    CONSTRAINT chk_resource_bookings_window_valid
        CHECK (start_time < end_time)
);

-- Overlap-query workhorse: only HOLD/CONFIRMED rows can ever conflict.
CREATE INDEX idx_resource_bookings_active_window
    ON resource_bookings (resource_uuid, start_time, end_time)
    WHERE status IN ('HOLD', 'CONFIRMED');

CREATE INDEX idx_resource_bookings_job_uuid
    ON resource_bookings (job_uuid)
    WHERE job_uuid IS NOT NULL;

CREATE INDEX idx_resource_bookings_class_definition_uuid
    ON resource_bookings (class_definition_uuid)
    WHERE class_definition_uuid IS NOT NULL;

CREATE INDEX idx_resource_bookings_scheduled_instance_uuid
    ON resource_bookings (scheduled_instance_uuid)
    WHERE scheduled_instance_uuid IS NOT NULL;

COMMENT ON TABLE organisation_resources IS 'Bookable physical resources (venues, equipment pools) registered by organisations';
COMMENT ON TABLE resource_availability_rules IS 'Calendar availability rules per resource: recurring open hours and recurring or one-off blackouts';
COMMENT ON TABLE resource_bookings IS 'Time-slot reservations of organisation resources: recruitment HOLDs from marketplace jobs and CONFIRMED bookings from scheduled classes';
