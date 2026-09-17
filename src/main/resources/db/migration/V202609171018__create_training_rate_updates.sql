-- An approved trainer proposes a full replacement rate card; the course or program creator approves
-- (copying it onto the application) or rejects it. Each row keeps the proposal and its decision.

CREATE TABLE course_training_rate_updates
(
    id                             BIGSERIAL PRIMARY KEY,
    uuid                           UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_uuid               UUID         NOT NULL REFERENCES course_training_applications (uuid) ON DELETE CASCADE,
    rate_currency                  VARCHAR(3)   NOT NULL REFERENCES currencies (code),
    private_online_hourly_rate     NUMERIC(12, 4),
    private_inperson_hourly_rate   NUMERIC(12, 4),
    group_online_hourly_rate       NUMERIC(12, 4),
    group_inperson_hourly_rate     NUMERIC(12, 4),
    private_online_session_rate    NUMERIC(12, 4),
    private_inperson_session_rate  NUMERIC(12, 4),
    group_online_session_rate      NUMERIC(12, 4),
    group_inperson_session_rate    NUMERIC(12, 4),
    private_online_daily_rate      NUMERIC(12, 4),
    private_inperson_daily_rate    NUMERIC(12, 4),
    group_online_daily_rate        NUMERIC(12, 4),
    group_inperson_daily_rate      NUMERIC(12, 4),
    note                           TEXT,
    status                         VARCHAR(16)  NOT NULL,
    review_notes                   TEXT,
    reviewed_by                    VARCHAR(255),
    reviewed_at                    TIMESTAMP,
    created_date                   TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by                     VARCHAR(255) NOT NULL,
    updated_date                   TIMESTAMP,
    updated_by                     VARCHAR(255),
    CONSTRAINT chk_course_training_rate_updates_status
        CHECK (UPPER(status) IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT chk_course_training_rate_updates_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_session_rate IS NULL OR private_online_session_rate > 0)
        AND (private_inperson_session_rate IS NULL OR private_inperson_session_rate > 0)
        AND (group_online_session_rate IS NULL OR group_online_session_rate > 0)
        AND (group_inperson_session_rate IS NULL OR group_inperson_session_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    )
);

-- At most one update awaits review per application.
CREATE UNIQUE INDEX uq_course_training_rate_updates_one_pending
    ON course_training_rate_updates (application_uuid)
    WHERE UPPER(status) = 'PENDING';

CREATE INDEX idx_course_training_rate_updates_application
    ON course_training_rate_updates (application_uuid, created_date DESC);

CREATE INDEX idx_course_training_rate_updates_status
    ON course_training_rate_updates (status);

CREATE TABLE program_training_rate_updates
(
    id                             BIGSERIAL PRIMARY KEY,
    uuid                           UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_uuid               UUID         NOT NULL REFERENCES program_training_applications (uuid) ON DELETE CASCADE,
    rate_currency                  VARCHAR(3)   NOT NULL REFERENCES currencies (code),
    private_online_hourly_rate     NUMERIC(12, 4),
    private_inperson_hourly_rate   NUMERIC(12, 4),
    group_online_hourly_rate       NUMERIC(12, 4),
    group_inperson_hourly_rate     NUMERIC(12, 4),
    private_online_session_rate    NUMERIC(12, 4),
    private_inperson_session_rate  NUMERIC(12, 4),
    group_online_session_rate      NUMERIC(12, 4),
    group_inperson_session_rate    NUMERIC(12, 4),
    private_online_daily_rate      NUMERIC(12, 4),
    private_inperson_daily_rate    NUMERIC(12, 4),
    group_online_daily_rate        NUMERIC(12, 4),
    group_inperson_daily_rate      NUMERIC(12, 4),
    note                           TEXT,
    status                         VARCHAR(16)  NOT NULL,
    review_notes                   TEXT,
    reviewed_by                    VARCHAR(255),
    reviewed_at                    TIMESTAMP,
    created_date                   TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by                     VARCHAR(255) NOT NULL,
    updated_date                   TIMESTAMP,
    updated_by                     VARCHAR(255),
    CONSTRAINT chk_program_training_rate_updates_status
        CHECK (UPPER(status) IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT chk_program_training_rate_updates_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_session_rate IS NULL OR private_online_session_rate > 0)
        AND (private_inperson_session_rate IS NULL OR private_inperson_session_rate > 0)
        AND (group_online_session_rate IS NULL OR group_online_session_rate > 0)
        AND (group_inperson_session_rate IS NULL OR group_inperson_session_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    )
);

-- At most one update awaits review per application.
CREATE UNIQUE INDEX uq_program_training_rate_updates_one_pending
    ON program_training_rate_updates (application_uuid)
    WHERE UPPER(status) = 'PENDING';

CREATE INDEX idx_program_training_rate_updates_application
    ON program_training_rate_updates (application_uuid, created_date DESC);

CREATE INDEX idx_program_training_rate_updates_status
    ON program_training_rate_updates (status);
