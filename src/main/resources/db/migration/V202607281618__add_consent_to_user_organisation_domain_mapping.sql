-- V202607281618__add_consent_to_user_organisation_domain_mapping.sql
-- Migration: Record how, when and by whom an organisation affiliation was consented to.
--
-- Until now a mapping row could be created unilaterally by an organisation with no act
-- of consent from the user at all. These columns make consent explicit and auditable,
-- and let a guardian consent on a minor's behalf.

ALTER TABLE user_organisation_domain_mapping
    ADD COLUMN consent_granted_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN consent_source              VARCHAR(32),
    ADD COLUMN consent_granted_by_user_uuid UUID,
    ADD COLUMN invitation_uuid             UUID;

ALTER TABLE user_organisation_domain_mapping
    ADD CONSTRAINT fk_user_org_domain_consent_granted_by
        FOREIGN KEY (consent_granted_by_user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,
    ADD CONSTRAINT fk_user_org_domain_invitation
        FOREIGN KEY (invitation_uuid) REFERENCES organisation_invitations (uuid) ON DELETE SET NULL,
    ADD CONSTRAINT chk_user_org_domain_consent_source
        CHECK (consent_source IS NULL
            OR consent_source IN ('INVITATION', 'GUARDIAN', 'SELF_JOIN', 'ADMIN', 'LEGACY'));

-- Existing affiliations pre-date consent capture. Mark them LEGACY rather than implying
-- a consent that was never given.
UPDATE user_organisation_domain_mapping
SET consent_source = 'LEGACY'
WHERE consent_source IS NULL;

CREATE INDEX idx_user_org_domain_invitation ON user_organisation_domain_mapping (invitation_uuid)
    WHERE invitation_uuid IS NOT NULL;
CREATE INDEX idx_user_org_domain_consent_source ON user_organisation_domain_mapping (consent_source);

COMMENT ON COLUMN user_organisation_domain_mapping.consent_granted_at IS 'When the affiliation was consented to; NULL for legacy rows created before consent capture';
COMMENT ON COLUMN user_organisation_domain_mapping.consent_source IS 'How consent was obtained: INVITATION, GUARDIAN (minor), SELF_JOIN, ADMIN, or LEGACY for pre-existing rows';
COMMENT ON COLUMN user_organisation_domain_mapping.consent_granted_by_user_uuid IS 'Who performed the consenting act - the user themselves, or their guardian for a minor';
