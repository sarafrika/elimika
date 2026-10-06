-- Students and parents need no admin approval; release any registered while the gate still held them.
UPDATE user_domain_mapping m
SET status      = 'APPROVED',
    reviewed_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
FROM user_domain d
WHERE d.uuid = m.domain_uuid
  AND d.domain_name IN ('student', 'parent')
  AND m.status = 'PENDING';
