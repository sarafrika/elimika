-- A class is delivered at a training branch (location). Record it explicitly so a class declares
-- its branch even before a specific venue is assigned; venue selection is then scoped to the branch.
ALTER TABLE class_definitions
    ADD COLUMN IF NOT EXISTS branch_uuid UUID;

-- Backfill from the assigned venue's branch where a venue is already set.
UPDATE class_definitions cd
SET branch_uuid = r.branch_uuid
FROM organisation_resources r
WHERE cd.venue_resource_uuid = r.uuid
  AND cd.branch_uuid IS NULL
  AND r.branch_uuid IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_class_definitions_branch_uuid
    ON class_definitions (branch_uuid);
