-- V202507092110__CreateInvitationsTable.sql
-- Migration: Create invitations table for email-based organization/branch invitations
-- This table manages the complete invitation lifecycle from creation to acceptance/decline

CREATE TABLE IF NOT EXISTS invitations
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    token             VARCHAR(64)  NOT NULL UNIQUE,
    recipient_email   VARCHAR(100) NOT NULL,
    recipient_name    VARCHAR(150) NOT NULL,
    organisation_uuid UUID         NOT NULL,
    branch_uuid       UUID,
    domain_uuid       UUID         NOT NULL,
    inviter_uuid      UUID         NOT NULL,
    inviter_name      VARCHAR(150) NOT NULL,
    status            VARCHAR(20)  NOT NULL        DEFAULT 'PENDING',
    expires_at        TIMESTAMP    NOT NULL,
    accepted_at       TIMESTAMP,
    declined_at       TIMESTAMP,
    user_uuid         UUID,
    notes             VARCHAR(500),
    created_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50)  NOT NULL,
    updated_by        VARCHAR(50),

    -- Foreign key constraints
    CONSTRAINT fk_invitation_organisation
        FOREIGN KEY (organisation_uuid) REFERENCES organisation (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_branch
        FOREIGN KEY (branch_uuid) REFERENCES training_branches (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_domain
        FOREIGN KEY (domain_uuid) REFERENCES user_domain (uuid) ON DELETE RESTRICT,
    CONSTRAINT fk_invitation_inviter
        FOREIGN KEY (inviter_uuid) REFERENCES users (uuid) ON DELETE RESTRICT,
    CONSTRAINT fk_invitation_user
        FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,

    -- Business rule constraints
    CONSTRAINT chk_invitation_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_invitation_dates
        CHECK (expires_at > created_date),
    CONSTRAINT chk_invitation_accepted_date
        CHECK (accepted_at IS NULL OR (status = 'ACCEPTED' AND accepted_at IS NOT NULL)),
    CONSTRAINT chk_invitation_declined_date
        CHECK (declined_at IS NULL OR (status = 'DECLINED' AND declined_at IS NOT NULL)),
    CONSTRAINT chk_invitation_user_accepted
        CHECK (user_uuid IS NULL OR (status = 'ACCEPTED' AND user_uuid IS NOT NULL)),
    CONSTRAINT chk_invitation_email_format
        CHECK (recipient_email ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Performance indexes for fast lookups
CREATE UNIQUE INDEX idx_invitations_uuid ON invitations (uuid);
CREATE UNIQUE INDEX idx_invitations_token ON invitations (token);
CREATE INDEX idx_invitations_recipient_email ON invitations (recipient_email);
CREATE INDEX idx_invitations_organisation_uuid ON invitations (organisation_uuid);
CREATE INDEX idx_invitations_branch_uuid ON invitations (branch_uuid) WHERE branch_uuid IS NOT NULL;
CREATE INDEX idx_invitations_domain_uuid ON invitations (domain_uuid);
CREATE INDEX idx_invitations_inviter_uuid ON invitations (inviter_uuid);
CREATE INDEX idx_invitations_user_uuid ON invitations (user_uuid) WHERE user_uuid IS NOT NULL;
CREATE INDEX idx_invitations_status ON invitations (status);
CREATE INDEX idx_invitations_expires_at ON invitations (expires_at);
CREATE INDEX idx_invitations_created_date ON invitations (created_date);

-- Composite indexes for common query patterns
CREATE INDEX idx_invitations_email_org_status ON invitations (recipient_email, organisation_uuid, status);
CREATE INDEX idx_invitations_email_org_branch_status ON invitations (recipient_email, organisation_uuid, branch_uuid, status)
    WHERE branch_uuid IS NOT NULL;
CREATE INDEX idx_invitations_status_expires ON invitations (status, expires_at);
CREATE INDEX idx_invitations_org_status_created ON invitations (organisation_uuid, status, created_date DESC);
CREATE INDEX idx_invitations_branch_status_created ON invitations (branch_uuid, status, created_date DESC)
    WHERE branch_uuid IS NOT NULL;
CREATE INDEX idx_invitations_inviter_created ON invitations (inviter_uuid, created_date DESC);


-- Comments for documentation
COMMENT ON TABLE invitations IS 'Stores organization and training branch invitations sent via email';
COMMENT ON COLUMN invitations.uuid IS 'Unique identifier for the invitation';
COMMENT ON COLUMN invitations.token IS 'Unique token for invitation acceptance/decline links (64 characters)';
COMMENT ON COLUMN invitations.recipient_email IS 'Email address of the invitation recipient';
COMMENT ON COLUMN invitations.recipient_name IS 'Full name of the invitation recipient';
COMMENT ON COLUMN invitations.organisation_uuid IS 'Organization the user is being invited to join';
COMMENT ON COLUMN invitations.branch_uuid IS 'Optional training branch for branch-specific invitations';
COMMENT ON COLUMN invitations.domain_uuid IS 'Role/domain being offered (student, instructor, admin, organisation_user)';
COMMENT ON COLUMN invitations.inviter_uuid IS 'User who sent the invitation';
COMMENT ON COLUMN invitations.inviter_name IS 'Name of the user who sent the invitation (cached for performance)';
COMMENT ON COLUMN invitations.status IS 'Current status: PENDING, ACCEPTED, DECLINED, EXPIRED, CANCELLED';
COMMENT ON COLUMN invitations.expires_at IS 'When the invitation expires (default 7 days from creation)';
COMMENT ON COLUMN invitations.accepted_at IS 'Timestamp when the invitation was accepted';
COMMENT ON COLUMN invitations.declined_at IS 'Timestamp when the invitation was declined';
COMMENT ON COLUMN invitations.user_uuid IS 'User who accepted the invitation (set when status = ACCEPTED)';
COMMENT ON COLUMN invitations.notes IS 'Optional notes or message for the invitation';
COMMENT ON COLUMN invitations.created_date IS 'When the invitation was created';
COMMENT ON COLUMN invitations.updated_date IS 'When the invitation was last updated';
COMMENT ON COLUMN invitations.created_by IS 'Email/identifier of who created the invitation';
COMMENT ON COLUMN invitations.updated_by IS 'Email/identifier of who last updated the invitation';