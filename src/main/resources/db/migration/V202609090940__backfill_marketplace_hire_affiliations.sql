-- Backfill organisation affiliations for instructors already hired through a marketplace class job.
--
-- Affiliation used to hang off the assignment endpoint rather than off the hire itself, so the
-- direct-hire path (a job posted with a preferred instructor) could produce a fully provisioned
-- class taught by somebody who never joined the organisation. The service now affiliates on every
-- hire path; this repairs any row that slipped through before it did.
--
-- An instructor who already holds ANY active mapping in the organisation - an org admin, say - is
-- skipped, so this can never demote an existing role nor breach uk_user_org_active_domain.
-- Idempotent: the NOT EXISTS guard means a re-run inserts nothing, and it affects zero rows today.

INSERT INTO user_organisation_domain_mapping
    (uuid, user_uuid, organisation_uuid, domain_uuid, branch_uuid,
     active, start_date, created_date, created_by, updated_date, deleted, consent_source)
SELECT gen_random_uuid(),
       hire.user_uuid,
       hire.organisation_uuid,
       (SELECT uuid FROM user_domain WHERE domain_name = 'instructor'),
       NULL,
       TRUE,
       CURRENT_DATE,
       CURRENT_TIMESTAMP,
       'backfill',
       CURRENT_TIMESTAMP,
       FALSE,
-- Consent was never captured for these hires, so they are stamped the way V202607281618 stamped
-- every other unconsented row rather than implying a consent nobody gave.
       'LEGACY'
-- DISTINCT over the pair only: an instructor hired for two jobs by the same organisation must
-- still produce a single active mapping.
FROM (SELECT DISTINCT i.user_uuid, j.organisation_uuid
      FROM class_marketplace_jobs j
               JOIN instructors i ON i.uuid = j.assigned_instructor_uuid
               JOIN users u ON u.uuid = i.user_uuid
      WHERE j.assigned_instructor_uuid IS NOT NULL
        AND i.user_uuid IS NOT NULL
        AND i.deleted = FALSE
        AND u.deleted = FALSE) hire
WHERE EXISTS (SELECT 1 FROM user_domain d WHERE d.domain_name = 'instructor')
  AND NOT EXISTS (SELECT 1
                  FROM user_organisation_domain_mapping m
                  WHERE m.user_uuid = hire.user_uuid
                    AND m.organisation_uuid = hire.organisation_uuid
                    AND m.active = TRUE
                    AND m.deleted = FALSE);
