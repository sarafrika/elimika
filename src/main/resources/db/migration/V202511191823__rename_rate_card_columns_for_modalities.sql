-- Align course training rate cards with modality-based combinations
ALTER TABLE course_training_applications
    RENAME COLUMN private_individual_rate TO private_online_rate;

ALTER TABLE course_training_applications
    RENAME COLUMN private_group_rate TO private_inperson_rate;

ALTER TABLE course_training_applications
    RENAME COLUMN public_individual_rate TO group_online_rate;

ALTER TABLE course_training_applications
    RENAME COLUMN public_group_rate TO group_inperson_rate;

ALTER TABLE course_training_applications
    DROP CONSTRAINT IF EXISTS chk_course_training_rate_card_non_negative;

ALTER TABLE course_training_applications
    ADD CONSTRAINT chk_course_training_rate_card_modalities_non_negative
        CHECK (
            private_online_rate >= 0
            AND private_inperson_rate >= 0
            AND group_online_rate >= 0
            AND group_inperson_rate >= 0
        );
