-- Give student groups the structure the organisation "Groups" page is built around:
-- which branch (campus) runs the group, which academic tier it sits at, and how many
-- students it is meant to hold. Until now all three lived inside the free-text `name`
-- ("Grade 9 · Stream A"), which cannot be filtered, sorted or counted against.
--
-- All three columns are nullable and there is deliberately NO backfill. Deriving a branch
-- or a tier by parsing legacy names is guesswork: separators differ per tenant, streams are
-- spelled a dozen ways, and a single unparseable row would abort the whole migration on a
-- live database. Legacy groups therefore keep NULLs and the frontend renders them under an
-- "Unassigned" pill until someone edits them, which is the honest representation of what we
-- actually know about those rows.
--
-- Also intentionally absent: a `student_groups.organisation_uuid -> organisation(uuid)` foreign
-- key and per-organisation name uniqueness. Both are correct in principle but both can fail
-- against existing production data (orphaned org uuids, duplicate names), so they need a data
-- audit and a cleanup migration of their own rather than being smuggled in here.

ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS branch_uuid UUID;
ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS tier_uuid UUID;
ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS capacity INTEGER;

-- Deleting a branch should not delete its cohorts: the students still exist and still need a
-- group, so the group simply becomes unassigned.
ALTER TABLE student_groups
    ADD CONSTRAINT fk_student_groups_branch
        FOREIGN KEY (branch_uuid) REFERENCES training_branches (uuid) ON DELETE SET NULL;

-- A tier is shared reference data. Removing one out from under live groups would silently
-- change what those groups mean, so the delete is refused instead.
ALTER TABLE student_groups
    ADD CONSTRAINT fk_student_groups_tier
        FOREIGN KEY (tier_uuid) REFERENCES academic_tiers (uuid) ON DELETE RESTRICT;

ALTER TABLE student_groups
    ADD CONSTRAINT chk_student_groups_capacity CHECK (capacity IS NULL OR capacity > 0);

-- Within one branch and one tier, the stream label is the thing that distinguishes groups, so
-- "Grade 9 / Stream A" may exist only once per campus. Partial, because legacy rows carry NULL
-- branch and tier and would otherwise all collide with each other.
CREATE UNIQUE INDEX uq_student_group_structured
    ON student_groups (organisation_uuid, branch_uuid, tier_uuid, lower(group_type))
    WHERE branch_uuid IS NOT NULL AND tier_uuid IS NOT NULL;

CREATE INDEX idx_student_groups_branch ON student_groups (branch_uuid);
CREATE INDEX idx_student_groups_tier ON student_groups (tier_uuid);

COMMENT ON COLUMN student_groups.branch_uuid IS
    'Training branch (campus) running this group; NULL for legacy groups that predate branch scoping.';
COMMENT ON COLUMN student_groups.tier_uuid IS
    'Academic tier (schooling level) this group sits at; NULL for legacy groups that predate tiers.';
COMMENT ON COLUMN student_groups.capacity IS
    'Intended size of the group. Advisory only: enrolment above it is reported, never blocked.';
COMMENT ON COLUMN student_groups.group_type IS
    'Stream label within a branch and tier, e.g. "Stream A" / "Blue". Free text by design.';

-- The column name lies and has already misled callers. It holds a users.uuid, not a
-- students.uuid: every writer feeds it uuids taken from the organisation user lookups
-- (getUsersByOrganisationAndDomain), and the roster query joins it straight to `users`.
-- Renaming it is a breaking change for a live table, so the contract is recorded here instead.
COMMENT ON COLUMN student_group_members.student_uuid IS
    'The student user''s users.uuid — NOT students.uuid. Joins to users(uuid); do not pass a students row identifier.';
