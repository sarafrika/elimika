-- When the user submitted the domain's onboarding for review (or, for domains without review, finished it).
ALTER TABLE user_domain_mapping
    ADD COLUMN submitted_at TIMESTAMP;

-- Course creators who already submitted their wallet keep that submission time.
UPDATE user_domain_mapping m
SET submitted_at = COALESCE(cc.submitted_at, cc.verification_requested_at)
FROM course_creators cc,
     user_domain d
WHERE d.uuid = m.domain_uuid
  AND d.domain_name = 'course_creator'
  AND cc.user_uuid = m.user_uuid
  AND UPPER(cc.verification_status) IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'REVOKED')
  AND COALESCE(cc.submitted_at, cc.verification_requested_at) IS NOT NULL;

-- Organisation admins whose organisation already asked for verification count as submitted.
UPDATE user_domain_mapping m
SET submitted_at = requested.requested_at
FROM (SELECT uodm.user_uuid, MIN(o.verification_requested_at) AS requested_at
      FROM user_organisation_domain_mapping uodm
               JOIN organisation o ON o.uuid = uodm.organisation_uuid
               JOIN user_domain ad ON ad.uuid = uodm.domain_uuid AND ad.domain_name = 'admin'
      WHERE uodm.active = TRUE
        AND uodm.deleted = FALSE
        AND o.verification_requested_at IS NOT NULL
      GROUP BY uodm.user_uuid) requested,
     user_domain d
WHERE d.uuid = m.domain_uuid
  AND d.domain_name = 'organisation_user'
  AND m.user_uuid = requested.user_uuid
  AND m.submitted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_domain_mapping_status_submitted
    ON user_domain_mapping (status, submitted_at);
