-- Organisations with exactly one live branch get their unbranched resources placed there.
UPDATE organisation_resources r
SET branch_uuid = ob.branch_uuid
FROM (SELECT organisation_uuid, (ARRAY_AGG(uuid))[1] AS branch_uuid
      FROM training_branches
      WHERE deleted = FALSE
      GROUP BY organisation_uuid
      HAVING COUNT(*) = 1) ob
WHERE r.organisation_uuid = ob.organisation_uuid
  AND r.branch_uuid IS NULL;

-- Classes booked into a now-branched venue inherit that branch.
UPDATE class_definitions cd
SET branch_uuid = r.branch_uuid
FROM organisation_resources r
WHERE cd.venue_resource_uuid = r.uuid
  AND cd.branch_uuid IS NULL
  AND r.branch_uuid IS NOT NULL;
