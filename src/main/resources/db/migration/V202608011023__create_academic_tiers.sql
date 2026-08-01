-- Academic tiers: the ordered schooling levels a student group belongs to.
--
-- The organisation "Groups" page needs to filter and sort cohorts by schooling level
-- ("Grade 7", "Form 2"). Today that level is buried inside the free-text group name, so it
-- cannot be filtered on, cannot be sorted in the order a school actually thinks in, and every
-- school spells it differently. A lookup table fixes all three: an explicit `tier_order` gives
-- the natural sequence (Kindergarten before PP1 before Grade 1), and a stable uuid lets a group
-- point at a level rather than restate it.
--
-- `tier_order` is gapped by tens on purpose. Curricula gain levels (a new pre-primary year, a
-- senior-school split) and renumbering an existing sequence would rewrite every row and break
-- any client that cached the order. Inserting at 15 or 145 never touches an existing row.
--
-- `organisation_uuid` is nullable and the seeded catalogue leaves it NULL: those are the
-- platform-wide Kenyan rows every tenant shares. The column exists so that a school running a
-- different curriculum can later add its own tiers with a plain INSERT, without a schema change.
-- Nothing reads or writes per-organisation tiers yet — the read API returns platform rows only.

CREATE TABLE academic_tiers
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    name              VARCHAR(100)             NOT NULL,
    tier_order        INTEGER                  NOT NULL,
    education_system  VARCHAR(20)              NOT NULL        DEFAULT 'KE',
    organisation_uuid UUID,
    active            BOOLEAN                  NOT NULL        DEFAULT true,
    description       TEXT,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by        VARCHAR(255)
);

-- Platform rows are the shared catalogue, so their names must be unique per education system.
-- Per-organisation rows are deliberately excluded: two schools may both define "Grade 7".
CREATE UNIQUE INDEX uq_academic_tiers_platform_name
    ON academic_tiers (lower(name), education_system)
    WHERE organisation_uuid IS NULL;

CREATE INDEX idx_academic_tiers_organisation ON academic_tiers (organisation_uuid);
CREATE INDEX idx_academic_tiers_order ON academic_tiers (education_system, tier_order);

COMMENT ON TABLE academic_tiers IS
    'Ordered schooling levels (Kindergarten, PP1, Grade 1, Form 4, ...) that student groups are filed under.';
COMMENT ON COLUMN academic_tiers.name IS
    'Display name of the level as a school would write it, e.g. "Grade 7".';
COMMENT ON COLUMN academic_tiers.tier_order IS
    'Sort position, gapped by tens so a new level can be inserted between two existing ones without renumbering.';
COMMENT ON COLUMN academic_tiers.education_system IS
    'Curriculum the level belongs to; ''KE'' is the seeded Kenyan system.';
COMMENT ON COLUMN academic_tiers.organisation_uuid IS
    'NULL for the shared platform catalogue; set only for a tier a single organisation defines for itself.';
COMMENT ON COLUMN academic_tiers.active IS
    'False retires a level from pickers without deleting groups that already reference it.';

INSERT INTO academic_tiers (name, tier_order, education_system, description)
VALUES ('Kindergarten', 10, 'KE', 'Pre-primary entry level'),
       ('PP1', 20, 'KE', 'Pre-primary 1'),
       ('PP2', 30, 'KE', 'Pre-primary 2'),
       ('Grade 1', 40, 'KE', 'Lower primary'),
       ('Grade 2', 50, 'KE', 'Lower primary'),
       ('Grade 3', 60, 'KE', 'Lower primary'),
       ('Grade 4', 70, 'KE', 'Upper primary'),
       ('Grade 5', 80, 'KE', 'Upper primary'),
       ('Grade 6', 90, 'KE', 'Upper primary'),
       ('Grade 7', 100, 'KE', 'Junior secondary'),
       ('Grade 8', 110, 'KE', 'Junior secondary'),
       ('Grade 9', 120, 'KE', 'Junior secondary'),
       ('Form 1', 130, 'KE', 'Senior secondary'),
       ('Form 2', 140, 'KE', 'Senior secondary'),
       ('Form 3', 150, 'KE', 'Senior secondary'),
       ('Form 4', 160, 'KE', 'Senior secondary');
