-- An organisation can invite a parent on behalf of a child under 18 who has no email of their own. The child
-- is named on the invitation and only created, as a learner with no login, when the parent approves. Such an
-- invitation has no recipient email: guardian_email is the parent's address and child_* describe the child.
ALTER TABLE organisation_invitations
    ALTER COLUMN recipient_email DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS child_first_name VARCHAR(50),
    ADD COLUMN IF NOT EXISTS child_last_name  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS child_dob        DATE;

ALTER TABLE organisation_invitations
    DROP CONSTRAINT IF EXISTS chk_org_invitation_recipient;
ALTER TABLE organisation_invitations
    ADD CONSTRAINT chk_org_invitation_recipient
        CHECK (recipient_email IS NOT NULL
            OR (child_first_name IS NOT NULL AND child_last_name IS NOT NULL AND child_dob IS NOT NULL
                AND guardian_email IS NOT NULL));

-- One live offer per child per parent per organisation, so siblings sharing a parent address each get their own.
CREATE UNIQUE INDEX IF NOT EXISTS uk_org_invitation_live_child
    ON organisation_invitations (organisation_uuid, lower(guardian_email), lower(child_first_name),
                                 lower(child_last_name), child_dob)
    WHERE child_dob IS NOT NULL AND status IN ('PENDING', 'AWAITING_GUARDIAN_CONSENT');
