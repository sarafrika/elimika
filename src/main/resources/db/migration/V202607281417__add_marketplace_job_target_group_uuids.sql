-- Links a marketplace job to the organisation student groups it targets.
--
-- The pre-existing `target_groups` column stays as the human-readable label snapshot shown on
-- adverts and instructor-facing views; this column carries the authoritative reference to rows in
-- `student_groups` so renaming a group never orphans the link. Stored comma-separated to match the
-- CSV convention already used by `target_groups` on this table.

ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS target_group_uuids TEXT;

COMMENT ON COLUMN class_marketplace_jobs.target_group_uuids IS
    'Comma-separated student_groups.uuid values the class is aimed at; target_groups holds the matching name snapshot.';
