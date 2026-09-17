-- The history of each course or program training application: every transition, rate update step and
-- the creator's first open, with the actor's name as it was at the time.

CREATE TABLE training_application_events
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_type VARCHAR(16)  NOT NULL,
    application_uuid UUID         NOT NULL,
    event_type       VARCHAR(40)  NOT NULL,
    actor_uuid       UUID,
    actor_name       VARCHAR(255),
    note             TEXT,
    created_date     TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by       VARCHAR(255) NOT NULL,
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_training_application_events_application_type
        CHECK (UPPER(application_type) IN ('COURSE', 'PROGRAM')),
    CONSTRAINT chk_training_application_events_event_type
        CHECK (UPPER(event_type) IN ('SUBMITTED', 'EDITED', 'OPENED_BY_CREATOR', 'APPROVED', 'REJECTED', 'REVOKED',
                                     'WITHDRAWN', 'RATES_UPDATE_SUBMITTED', 'RATES_UPDATE_APPROVED',
                                     'RATES_UPDATE_REJECTED', 'RATES_UPDATE_WITHDRAWN'))
);

CREATE INDEX idx_training_application_events_application
    ON training_application_events (application_type, application_uuid, created_date DESC);

-- The creator's first open is recorded once per application, however many times it is read.
CREATE UNIQUE INDEX uq_training_application_events_first_open
    ON training_application_events (application_type, application_uuid)
    WHERE event_type = 'OPENED_BY_CREATOR';
