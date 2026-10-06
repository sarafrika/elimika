-- Guardians a student names during onboarding. Each row either became an active guardian link
-- (the email already had an account) or carries a pending emailed invitation the guardian claims.

CREATE TABLE IF NOT EXISTS student_guardian_contacts
(
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    student_uuid            UUID         NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,
    guardian_email          VARCHAR(150) NOT NULL,
    guardian_name           VARCHAR(150) NOT NULL,
    guardian_phone          VARCHAR(50),
    relationship_type       VARCHAR(20)  NOT NULL DEFAULT 'GUARDIAN',
    position                INTEGER      NOT NULL DEFAULT 1,
    contact_status          VARCHAR(20)  NOT NULL DEFAULT 'INVITED',
    guardian_user_uuid      UUID REFERENCES users (uuid) ON DELETE SET NULL,
    link_uuid               UUID REFERENCES student_guardian_links (uuid) ON DELETE SET NULL,
    token_hash              VARCHAR(64) UNIQUE,
    invitation_expires_at   TIMESTAMP,
    invitation_sent_at      TIMESTAMP,
    invitation_send_count   INTEGER      NOT NULL DEFAULT 0,
    linked_at               TIMESTAMP,
    declined_at             TIMESTAMP,
    removed_at              TIMESTAMP,
    invited_by              UUID REFERENCES users (uuid) ON DELETE SET NULL,
    created_date            TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by              VARCHAR(255) NOT NULL,
    updated_date            TIMESTAMP,
    updated_by              VARCHAR(255),
    CONSTRAINT chk_student_guardian_contacts_status
        CHECK (contact_status IN ('INVITED', 'LINKED', 'DECLINED', 'REMOVED')),
    CONSTRAINT chk_student_guardian_contacts_relationship
        CHECK (relationship_type IN ('PARENT', 'GUARDIAN', 'SPONSOR')),
    CONSTRAINT chk_student_guardian_contacts_email_lower
        CHECK (guardian_email = LOWER(guardian_email))
);

-- One live entry per guardian email per student; removed entries are kept for audit.
CREATE UNIQUE INDEX IF NOT EXISTS uk_student_guardian_contacts_live_email
    ON student_guardian_contacts (student_uuid, guardian_email)
    WHERE contact_status <> 'REMOVED';

CREATE INDEX IF NOT EXISTS idx_student_guardian_contacts_student
    ON student_guardian_contacts (student_uuid);

CREATE INDEX IF NOT EXISTS idx_student_guardian_contacts_email_status
    ON student_guardian_contacts (guardian_email, contact_status);

COMMENT ON TABLE student_guardian_contacts IS
    'Guardians named by a student; the only route by which a parent gains access.';
