-- Backfill org-scoped manager mappings for organisations that currently have NO active manager.
--
-- A "manager" is a user with an active, non-deleted org-scoped mapping whose domain is
-- 'organisation_user' or 'admin'. Some legacy organisations were created before the creator
-- was auto-assigned an org-scoped 'admin' mapping (or were created with a null creator), leaving
-- them with no one able to manage them. This migration repairs those orgs.
--
-- NOTE on organisation.created_by: the audit column stores authentication.getName(), which for a
-- Keycloak JWT is the subject claim ('sub') == users.keycloak_id. We therefore match a creator by
-- joining users.keycloak_id = organisation.created_by (values like 'SYSTEM'/'system' simply won't
-- match a real user and fall through to the fallback rule).
--
-- All statements are guarded with NOT EXISTS / uniqueness checks so the migration is idempotent
-- and can never create a second active manager for an org.

-- ---------------------------------------------------------------------------
-- PRIMARY (a) : creator is a valid, non-deleted user AND has NO active mapping
--               in the org yet -> INSERT a fresh active 'admin' mapping.
-- ---------------------------------------------------------------------------
INSERT INTO user_organisation_domain_mapping
    (uuid, user_uuid, organisation_uuid, domain_uuid, branch_uuid,
     active, start_date, created_date, created_by, updated_date, deleted)
SELECT gen_random_uuid(),
       u.uuid,
       o.uuid,
       (SELECT uuid FROM user_domain WHERE domain_name = 'admin'),
       NULL,
       TRUE,
       CURRENT_DATE,
       CURRENT_TIMESTAMP,
       'backfill',
       CURRENT_TIMESTAMP,
       FALSE
FROM organisation o
JOIN users u
    ON u.keycloak_id = o.created_by
   AND u.deleted = FALSE
WHERE o.deleted = FALSE
  -- org currently has no active manager
  AND NOT EXISTS (
        SELECT 1
        FROM user_organisation_domain_mapping m
        JOIN user_domain d ON d.uuid = m.domain_uuid
        WHERE m.organisation_uuid = o.uuid
          AND m.active = TRUE
          AND m.deleted = FALSE
          AND d.domain_name IN ('organisation_user', 'admin')
      )
  -- creator has no active mapping in this org yet (respect the unique active-mapping index)
  AND NOT EXISTS (
        SELECT 1
        FROM user_organisation_domain_mapping m2
        WHERE m2.organisation_uuid = o.uuid
          AND m2.user_uuid = u.uuid
          AND m2.active = TRUE
          AND m2.deleted = FALSE
      );

-- ---------------------------------------------------------------------------
-- PRIMARY (b) : creator is a valid, non-deleted user who IS already an active
--               member (with a non-manager role) -> PROMOTE that existing
--               active mapping to 'admin' (upsert path, avoids a duplicate
--               active row that would violate uk_user_org_active_domain).
-- ---------------------------------------------------------------------------
UPDATE user_organisation_domain_mapping m
SET domain_uuid  = (SELECT uuid FROM user_domain WHERE domain_name = 'admin'),
    updated_by   = 'backfill',
    updated_date = CURRENT_TIMESTAMP
FROM organisation o,
     users u
WHERE m.organisation_uuid = o.uuid
  AND m.user_uuid = u.uuid
  AND o.deleted = FALSE
  AND u.deleted = FALSE
  AND u.keycloak_id = o.created_by
  AND m.active = TRUE
  AND m.deleted = FALSE
  -- org currently has no active manager (the creator's own active row is a non-manager role)
  AND NOT EXISTS (
        SELECT 1
        FROM user_organisation_domain_mapping mm
        JOIN user_domain d ON d.uuid = mm.domain_uuid
        WHERE mm.organisation_uuid = o.uuid
          AND mm.active = TRUE
          AND mm.deleted = FALSE
          AND d.domain_name IN ('organisation_user', 'admin')
      );

-- ---------------------------------------------------------------------------
-- FALLBACK : created_by is NOT a valid user AND the org has EXACTLY ONE active
--            member -> promote that sole member to 'admin' so the org is usable.
--            (An org with a single member and no manager is otherwise unusable.)
--
--            Orgs with MULTIPLE members and no valid creator are intentionally
--            LEFT UNTOUCHED (safer) and must be repaired manually via
--            PUT /api/v1/organisations/{uuid}/users/{userUuid}/domain.
-- ---------------------------------------------------------------------------
UPDATE user_organisation_domain_mapping m
SET domain_uuid  = (SELECT uuid FROM user_domain WHERE domain_name = 'admin'),
    updated_by   = 'backfill',
    updated_date = CURRENT_TIMESTAMP
FROM organisation o
WHERE m.organisation_uuid = o.uuid
  AND o.deleted = FALSE
  AND m.active = TRUE
  AND m.deleted = FALSE
  -- org currently has no active manager
  AND NOT EXISTS (
        SELECT 1
        FROM user_organisation_domain_mapping mm
        JOIN user_domain d ON d.uuid = mm.domain_uuid
        WHERE mm.organisation_uuid = o.uuid
          AND mm.active = TRUE
          AND mm.deleted = FALSE
          AND d.domain_name IN ('organisation_user', 'admin')
      )
  -- created_by is NOT a valid existing (non-deleted) user
  AND NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.keycloak_id = o.created_by
          AND u.deleted = FALSE
      )
  -- the org has EXACTLY ONE active (non-deleted) member
  AND (
        SELECT COUNT(DISTINCT m3.user_uuid)
        FROM user_organisation_domain_mapping m3
        WHERE m3.organisation_uuid = o.uuid
          AND m3.active = TRUE
          AND m3.deleted = FALSE
      ) = 1;
