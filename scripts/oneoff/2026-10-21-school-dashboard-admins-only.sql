-- One-off: only school admins operate the school dashboard. Removes the umbrella organisation_user domain from
-- everyone who is not an active admin of a school (learners, trainers, parents, non-admin members); removed rows
-- are kept in backup_umbrella_organisation_user. Preview: psql -v ON_ERROR_STOP=1 -f this.sql   Apply: -v apply=1.
\if :{?apply}
\else
\set apply 0
\endif

BEGIN;

CREATE TEMP TABLE umbrella_to_remove ON COMMIT DROP AS
SELECT udm.*
FROM user_domain_mapping udm
         JOIN user_domain umbrella ON umbrella.uuid = udm.domain_uuid AND umbrella.domain_name = 'organisation_user'
WHERE NOT EXISTS (SELECT 1
                  FROM user_organisation_domain_mapping m
                           JOIN user_domain d ON d.uuid = m.domain_uuid AND d.domain_name = 'admin'
                  WHERE m.user_uuid = udm.user_uuid
                    AND m.active
                    AND NOT COALESCE(m.deleted, false));

-- Preview: who loses the school dashboard, and the school roles they keep as community members.
SELECT u.email,
       udm.status                                                                  AS umbrella_status,
       COALESCE(string_agg(DISTINCT d.domain_name, ', ') FILTER (WHERE m.active), '—') AS school_roles_kept
FROM umbrella_to_remove udm
         JOIN users u ON u.uuid = udm.user_uuid
         LEFT JOIN user_organisation_domain_mapping m ON m.user_uuid = udm.user_uuid AND NOT COALESCE(m.deleted, false)
         LEFT JOIN user_domain d ON d.uuid = m.domain_uuid
GROUP BY u.email, udm.status
ORDER BY u.email;

SELECT count(*) AS umbrella_domains_to_remove FROM umbrella_to_remove;

\if :apply
CREATE TABLE IF NOT EXISTS backup_umbrella_organisation_user AS SELECT * FROM user_domain_mapping WHERE false;
INSERT INTO backup_umbrella_organisation_user SELECT * FROM umbrella_to_remove
ON CONFLICT DO NOTHING;
DELETE FROM user_domain_mapping udm USING umbrella_to_remove r WHERE udm.id = r.id;
SELECT 'removed' AS result, count(*) FROM umbrella_to_remove;
COMMIT;
\else
ROLLBACK;
\endif
