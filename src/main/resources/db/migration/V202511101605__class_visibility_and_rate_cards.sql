-- Enforce explicit visibility and session format on class definitions.
ALTER TABLE class_definitions
    ADD COLUMN class_visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN session_format   VARCHAR(16) NOT NULL DEFAULT 'GROUP';

-- Drop defaults after backfilling legacy rows.
ALTER TABLE class_definitions
    ALTER COLUMN class_visibility DROP DEFAULT,
    ALTER COLUMN session_format DROP DEFAULT;

-- Expand course training applications to capture segmented rate cards.
ALTER TABLE course_training_applications
    ADD COLUMN private_individual_rate NUMERIC(12, 4),
    ADD COLUMN private_group_rate      NUMERIC(12, 4),
    ADD COLUMN public_individual_rate  NUMERIC(12, 4),
    ADD COLUMN public_group_rate       NUMERIC(12, 4);

-- Seed the new rate card columns using the legacy single-rate column.
UPDATE course_training_applications
SET private_individual_rate = COALESCE(rate_per_hour_per_head, 0),
    private_group_rate      = COALESCE(rate_per_hour_per_head, 0),
    public_individual_rate  = COALESCE(rate_per_hour_per_head, 0),
    public_group_rate       = COALESCE(rate_per_hour_per_head, 0);

-- Guard against nulls/non-positive values now that data has been migrated.
ALTER TABLE course_training_applications
    ALTER COLUMN private_individual_rate SET NOT NULL,
    ALTER COLUMN private_group_rate SET NOT NULL,
    ALTER COLUMN public_individual_rate SET NOT NULL,
    ALTER COLUMN public_group_rate SET NOT NULL,
    ADD CONSTRAINT chk_course_training_rate_card_non_negative
        CHECK (
            private_individual_rate >= 0
            AND private_group_rate >= 0
            AND public_individual_rate >= 0
            AND public_group_rate >= 0
        );

-- Remove the deprecated rate column now that the rate card is in place.
ALTER TABLE course_training_applications
    DROP COLUMN rate_per_hour_per_head;
