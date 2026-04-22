CREATE TABLE class_marketplace_jobs
(
    id                           BIGSERIAL PRIMARY KEY,
    uuid                         UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    organisation_uuid            UUID         NOT NULL REFERENCES organisation (uuid),
    course_uuid                  UUID         NOT NULL REFERENCES courses (uuid),
    title                        VARCHAR(255) NOT NULL,
    description                  TEXT,
    status                       VARCHAR(32)  NOT NULL,
    class_visibility             VARCHAR(16)  NOT NULL,
    session_format               VARCHAR(16)  NOT NULL,
    default_start_time           TIMESTAMP    NOT NULL,
    default_end_time             TIMESTAMP    NOT NULL,
    academic_period_start_date   DATE,
    academic_period_end_date     DATE,
    registration_period_start_date DATE,
    registration_period_end_date DATE,
    class_reminder_minutes       INTEGER,
    class_color                  VARCHAR(7),
    location_type                VARCHAR(32)  NOT NULL,
    location_name                VARCHAR(255),
    location_latitude            NUMERIC(9, 6),
    location_longitude           NUMERIC(9, 6),
    meeting_link                 VARCHAR(1000),
    max_participants             INTEGER      NOT NULL,
    allow_waitlist               BOOLEAN      NOT NULL DEFAULT TRUE,
    assigned_instructor_uuid     UUID,
    assigned_application_uuid    UUID,
    assigned_class_definition_uuid UUID REFERENCES class_definitions (uuid),
    filled_at                    TIMESTAMP,
    created_date                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date                 TIMESTAMP,
    created_by                   VARCHAR(255) NOT NULL,
    updated_by                   VARCHAR(255),
    CONSTRAINT chk_class_marketplace_jobs_status
        CHECK (status IN ('OPEN', 'FILLED', 'CANCELLED')),
    CONSTRAINT chk_class_marketplace_jobs_visibility
        CHECK (class_visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_class_marketplace_jobs_session_format
        CHECK (session_format IN ('INDIVIDUAL', 'GROUP')),
    CONSTRAINT chk_class_marketplace_jobs_location_type
        CHECK (location_type IN ('ONLINE', 'IN_PERSON', 'HYBRID')),
    CONSTRAINT chk_class_marketplace_jobs_time_valid
        CHECK (default_start_time < default_end_time),
    CONSTRAINT chk_class_marketplace_jobs_academic_period_valid
        CHECK (
            academic_period_start_date IS NULL
            OR academic_period_end_date IS NULL
            OR academic_period_start_date <= academic_period_end_date
        ),
    CONSTRAINT chk_class_marketplace_jobs_registration_period_valid
        CHECK (
            registration_period_start_date IS NULL
            OR registration_period_end_date IS NULL
            OR registration_period_start_date <= registration_period_end_date
        ),
    CONSTRAINT chk_class_marketplace_jobs_class_reminder_minutes_non_negative
        CHECK (class_reminder_minutes IS NULL OR class_reminder_minutes >= 0),
    CONSTRAINT chk_class_marketplace_jobs_class_color_valid
        CHECK (class_color IS NULL OR class_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_class_marketplace_jobs_max_participants_positive
        CHECK (max_participants > 0)
);

CREATE INDEX idx_class_marketplace_jobs_organisation_uuid
    ON class_marketplace_jobs (organisation_uuid);

CREATE INDEX idx_class_marketplace_jobs_course_uuid
    ON class_marketplace_jobs (course_uuid);

CREATE INDEX idx_class_marketplace_jobs_status
    ON class_marketplace_jobs (status);

CREATE TABLE class_marketplace_job_session_templates
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    job_uuid            UUID         NOT NULL REFERENCES class_marketplace_jobs (uuid) ON DELETE CASCADE,
    start_time          TIMESTAMP    NOT NULL,
    end_time            TIMESTAMP    NOT NULL,
    recurrence_type     VARCHAR(32),
    interval_value      INTEGER,
    days_of_week        VARCHAR(128),
    day_of_month        INTEGER,
    end_date            DATE,
    occurrence_count    INTEGER,
    conflict_resolution VARCHAR(32)  NOT NULL,
    created_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    created_by          VARCHAR(255) NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_class_marketplace_job_session_templates_time_valid
        CHECK (start_time < end_time),
    CONSTRAINT chk_class_marketplace_job_session_templates_recurrence_type
        CHECK (recurrence_type IS NULL OR recurrence_type IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_class_marketplace_job_session_templates_conflict_resolution
        CHECK (conflict_resolution IN ('FAIL', 'SKIP', 'ROLLOVER'))
);

CREATE INDEX idx_class_marketplace_job_session_templates_job_uuid
    ON class_marketplace_job_session_templates (job_uuid);

CREATE TABLE class_marketplace_job_applications
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    job_uuid        UUID        NOT NULL REFERENCES class_marketplace_jobs (uuid) ON DELETE CASCADE,
    instructor_uuid UUID        NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    application_note TEXT,
    review_notes   TEXT,
    reviewed_by    VARCHAR(255),
    reviewed_at    TIMESTAMP,
    created_date   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP,
    created_by     VARCHAR(255) NOT NULL,
    updated_by     VARCHAR(255),
    CONSTRAINT uq_class_marketplace_job_application UNIQUE (job_uuid, instructor_uuid),
    CONSTRAINT chk_class_marketplace_job_applications_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ASSIGNED', 'NOT_SELECTED'))
);

CREATE INDEX idx_class_marketplace_job_applications_job_uuid
    ON class_marketplace_job_applications (job_uuid);

CREATE INDEX idx_class_marketplace_job_applications_instructor_uuid
    ON class_marketplace_job_applications (instructor_uuid);

CREATE INDEX idx_class_marketplace_job_applications_status
    ON class_marketplace_job_applications (status);

COMMENT ON TABLE class_marketplace_jobs IS 'Organisation-posted class adverts awaiting instructor assignment before real class creation';
COMMENT ON TABLE class_marketplace_job_session_templates IS 'Stored schedule templates copied into the actual class once an instructor is assigned';
COMMENT ON TABLE class_marketplace_job_applications IS 'Instructor applications for organisation-posted marketplace class jobs';
