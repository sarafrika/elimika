-- One row per self-registration started in Elimika. Personal details live in Keycloak (mirrored
-- read-only into users); this keeps only what Elimika itself needs: which domain was asked for,
-- when the terms were accepted, and when the set-password email went out.
CREATE TABLE IF NOT EXISTS account_registrations
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid             UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    requested_domain      VARCHAR(40)  NOT NULL,
    terms_accepted_at     TIMESTAMP    NOT NULL,
    actions_email_sent_at TIMESTAMP,
    created_date          TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by            VARCHAR(255) NOT NULL,
    updated_date          TIMESTAMP,
    updated_by            VARCHAR(255),
    CONSTRAINT chk_account_registrations_domain
        CHECK (LOWER(requested_domain) IN ('student', 'instructor', 'course_creator', 'parent', 'organisation_user'))
);

CREATE INDEX IF NOT EXISTS idx_account_registrations_user_uuid ON account_registrations (user_uuid);

COMMENT ON TABLE account_registrations IS 'Self-registrations started in Elimika; identity itself is held by Keycloak.';
