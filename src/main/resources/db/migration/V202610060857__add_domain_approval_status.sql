-- A domain mapping now grants access only once a platform admin has approved it. Every mapping
-- that exists today was already granting access, so it is backfilled as APPROVED; the column
-- default keeps rows inserted outside the application (fixtures, manual repairs) behaving the
-- same way, while the application itself always writes an explicit status.
ALTER TABLE user_domain_mapping
    ADD COLUMN IF NOT EXISTS status        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS reviewed_at   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reviewed_by   UUID,
    ADD COLUMN IF NOT EXISTS review_reason TEXT;

UPDATE user_domain_mapping
SET status = 'APPROVED'
WHERE status IS NULL;

ALTER TABLE user_domain_mapping
    ALTER COLUMN status SET DEFAULT 'APPROVED',
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE user_domain_mapping
    DROP CONSTRAINT IF EXISTS chk_user_domain_mapping_status;

ALTER TABLE user_domain_mapping
    ADD CONSTRAINT chk_user_domain_mapping_status
        CHECK (UPPER(status) IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'));

CREATE INDEX IF NOT EXISTS idx_user_domain_mapping_status
    ON user_domain_mapping (status, created_at);

COMMENT ON COLUMN user_domain_mapping.status IS 'Approval state of the domain; only APPROVED grants access.';
COMMENT ON COLUMN user_domain_mapping.reviewed_by IS 'User uuid of the platform admin who last decided the request.';
