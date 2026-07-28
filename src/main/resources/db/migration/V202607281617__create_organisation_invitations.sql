-- V202607281617__create_organisation_invitations.sql
-- Migration: Create organisation invitation tables.
--
-- An invitation is an OFFER addressed to an email address. It is deliberately kept
-- separate from user_organisation_domain_mapping (the affiliation): the mapping row is
-- written only when the invitee - or, for a minor, their guardian - explicitly accepts.
-- This replaces the invitation tables dropped in V202512111407.

CREATE TABLE organisation_invitations
(
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    -- Only the hash of the token is stored; the raw token exists solely in the emailed link.
    token_hash                  VARCHAR(128)             NOT NULL UNIQUE,

    organisation_uuid           UUID                     NOT NULL,
    branch_uuid                 UUID,
    domain_uuid                 UUID                     NOT NULL,

    recipient_email             VARCHAR(150)             NOT NULL,
    recipient_name              VARCHAR(150),
    -- Resolved server-side at send time when the email already belongs to a platform user.
    recipient_user_uuid         UUID,

    inviter_user_uuid           UUID                     NOT NULL,

    status                      VARCHAR(32)              NOT NULL        DEFAULT 'PENDING',
    message                     VARCHAR(2000),

    expires_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at                 TIMESTAMP WITH TIME ZONE,
    declined_at                 TIMESTAMP WITH TIME ZONE,
    revoked_at                  TIMESTAMP WITH TIME ZONE,
    accepted_user_uuid          UUID,

    -- Guardian consent branch, populated only when the invitee declares a minor date of
    -- birth. The date of birth itself is never stored here and is never exposed to the
    -- organisation; only the consent state is.
    requires_guardian_consent   BOOLEAN                  NOT NULL        DEFAULT FALSE,
    guardian_email              VARCHAR(150),
    guardian_name               VARCHAR(150),
    guardian_relationship_type  VARCHAR(50),
    guardian_phone              VARCHAR(50),
    guardian_consent_token_hash VARCHAR(128) UNIQUE,
    guardian_consented_at       TIMESTAMP WITH TIME ZONE,
    guardian_user_uuid          UUID,

    created_date                TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date                TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by                  VARCHAR(255)             NOT NULL,
    updated_by                  VARCHAR(255),

    CONSTRAINT fk_org_invitation_organisation
        FOREIGN KEY (organisation_uuid) REFERENCES organisation (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_org_invitation_branch
        FOREIGN KEY (branch_uuid) REFERENCES training_branches (uuid) ON DELETE SET NULL,
    CONSTRAINT fk_org_invitation_domain
        FOREIGN KEY (domain_uuid) REFERENCES user_domain (uuid) ON DELETE RESTRICT,
    CONSTRAINT fk_org_invitation_recipient_user
        FOREIGN KEY (recipient_user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,
    CONSTRAINT fk_org_invitation_inviter
        FOREIGN KEY (inviter_user_uuid) REFERENCES users (uuid) ON DELETE RESTRICT,
    CONSTRAINT fk_org_invitation_accepted_user
        FOREIGN KEY (accepted_user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,
    CONSTRAINT fk_org_invitation_guardian_user
        FOREIGN KEY (guardian_user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,

    CONSTRAINT chk_org_invitation_status
        CHECK (status IN ('PENDING', 'AWAITING_GUARDIAN_CONSENT', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_org_invitation_guardian_relationship
        CHECK (guardian_relationship_type IS NULL
            OR guardian_relationship_type IN ('PARENT', 'GUARDIAN', 'SPONSOR')),
    CONSTRAINT chk_org_invitation_email_format
        CHECK (recipient_email ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- One live offer per email per organisation. Terminal states are excluded so an
-- organisation can re-invite after a decline, revocation or expiry.
CREATE UNIQUE INDEX uk_org_invitation_live_recipient
    ON organisation_invitations (organisation_uuid, lower(recipient_email))
    WHERE status IN ('PENDING', 'AWAITING_GUARDIAN_CONSENT');

CREATE INDEX idx_org_invitation_organisation ON organisation_invitations (organisation_uuid);
CREATE INDEX idx_org_invitation_recipient_email ON organisation_invitations (lower(recipient_email));
CREATE INDEX idx_org_invitation_recipient_user ON organisation_invitations (recipient_user_uuid);
CREATE INDEX idx_org_invitation_status ON organisation_invitations (status);
CREATE INDEX idx_org_invitation_expires_at ON organisation_invitations (expires_at);
CREATE INDEX idx_org_invitation_guardian_email ON organisation_invitations (lower(guardian_email))
    WHERE guardian_email IS NOT NULL;

COMMENT ON TABLE organisation_invitations IS 'Token-bearing offers to join an organisation; the affiliation is written only on accept';
COMMENT ON COLUMN organisation_invitations.token_hash IS 'Hash of the invitation token; the raw token is only ever in the emailed link';
COMMENT ON COLUMN organisation_invitations.recipient_user_uuid IS 'Set when the invited email already belongs to a platform user';
COMMENT ON COLUMN organisation_invitations.requires_guardian_consent IS 'True when the invitee declared a date of birth below the configured age gate';
COMMENT ON COLUMN organisation_invitations.guardian_consent_token_hash IS 'Separate token issued to the guardian so they consent from their own link';

-- Classes named in the invitation. These are SURFACED to the invitee on acceptance,
-- never auto-enrolled: enrolment stays a separate, paywall-aware action.
CREATE TABLE organisation_invitation_classes
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    invitation_uuid       UUID                     NOT NULL,
    class_definition_uuid UUID                     NOT NULL,

    created_date          TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date          TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by            VARCHAR(255)             NOT NULL,
    updated_by            VARCHAR(255),

    CONSTRAINT fk_org_invitation_class_invitation
        FOREIGN KEY (invitation_uuid) REFERENCES organisation_invitations (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_org_invitation_class_definition
        FOREIGN KEY (class_definition_uuid) REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    CONSTRAINT uk_org_invitation_class UNIQUE (invitation_uuid, class_definition_uuid)
);

CREATE INDEX idx_org_invitation_class_invitation ON organisation_invitation_classes (invitation_uuid);
CREATE INDEX idx_org_invitation_class_definition ON organisation_invitation_classes (class_definition_uuid);

COMMENT ON TABLE organisation_invitation_classes IS 'Classes named in an invitation; surfaced to the invitee on acceptance, never auto-enrolled';
