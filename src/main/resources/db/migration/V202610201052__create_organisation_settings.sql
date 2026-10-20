-- Settings an organisation chooses for itself; one row per organisation, created on first change.
CREATE TABLE organisation_settings
(
    id                         BIGSERIAL PRIMARY KEY,
    uuid                       UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid          UUID                     NOT NULL UNIQUE REFERENCES organisation (uuid) ON DELETE CASCADE,
    session_report_grace_hours INTEGER                  NOT NULL DEFAULT 48,
    created_date               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date               TIMESTAMP WITH TIME ZONE,
    created_by                 VARCHAR(255)             NOT NULL,
    updated_by                 VARCHAR(255),
    CONSTRAINT chk_organisation_settings_grace_hours CHECK (session_report_grace_hours BETWEEN 1 AND 720)
);

COMMENT ON COLUMN organisation_settings.session_report_grace_hours IS
    'Hours an instructor may still edit a submitted session report before it locks.';
