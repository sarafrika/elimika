-- A marketplace job is delivered at one training branch; legacy rows stay nullable and the service enforces it.
ALTER TABLE class_marketplace_jobs
    ADD COLUMN IF NOT EXISTS branch_uuid UUID REFERENCES training_branches (uuid);

-- 1. The branch of the job's venue, when that branch belongs to the job's organisation.
UPDATE class_marketplace_jobs j
SET branch_uuid = r.branch_uuid
FROM class_marketplace_job_resources jr
         JOIN organisation_resources r ON r.uuid = jr.resource_uuid
         JOIN training_branches b ON b.uuid = r.branch_uuid
WHERE jr.job_uuid = j.uuid
  AND r.resource_type = 'VENUE'
  AND b.organisation_uuid = j.organisation_uuid
  AND j.branch_uuid IS NULL;

-- 2. Otherwise the single branch every branched resource of the job agrees on.
UPDATE class_marketplace_jobs j
SET branch_uuid = agreed.branch_uuid
FROM (SELECT jr.job_uuid, (ARRAY_AGG(DISTINCT r.branch_uuid))[1] AS branch_uuid
      FROM class_marketplace_job_resources jr
               JOIN organisation_resources r ON r.uuid = jr.resource_uuid
      WHERE r.branch_uuid IS NOT NULL
      GROUP BY jr.job_uuid
      HAVING COUNT(DISTINCT r.branch_uuid) = 1) agreed
         JOIN training_branches b ON b.uuid = agreed.branch_uuid
WHERE j.uuid = agreed.job_uuid
  AND b.organisation_uuid = j.organisation_uuid
  AND j.branch_uuid IS NULL;

-- 3. Otherwise the organisation's only live branch.
UPDATE class_marketplace_jobs j
SET branch_uuid = ob.branch_uuid
FROM (SELECT organisation_uuid, (ARRAY_AGG(uuid))[1] AS branch_uuid
      FROM training_branches
      WHERE deleted = FALSE
      GROUP BY organisation_uuid
      HAVING COUNT(*) = 1) ob
WHERE j.organisation_uuid = ob.organisation_uuid
  AND j.branch_uuid IS NULL;

-- Classes created from a job inherit its branch when they have none of their own.
UPDATE class_definitions cd
SET branch_uuid = j.branch_uuid
FROM class_marketplace_jobs j
WHERE cd.marketplace_job_uuid = j.uuid
  AND cd.branch_uuid IS NULL
  AND j.branch_uuid IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_class_marketplace_jobs_branch_uuid
    ON class_marketplace_jobs (branch_uuid);
