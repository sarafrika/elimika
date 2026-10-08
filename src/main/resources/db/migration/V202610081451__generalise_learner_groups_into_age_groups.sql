-- Learner groups become age groups: one table for every named age band, owned by an instructor's or
-- organisation's saved set, or copied into a course or program training application.

ALTER TABLE training_application_learner_groups RENAME TO age_groups;
ALTER SEQUENCE training_application_learner_groups_id_seq RENAME TO age_groups_id_seq;
ALTER TABLE age_groups RENAME COLUMN application_type TO owner_type;
ALTER TABLE age_groups RENAME COLUMN application_uuid TO owner_uuid;

ALTER TABLE age_groups DROP CONSTRAINT chk_training_application_learner_groups_application_type;
ALTER TABLE age_groups ALTER COLUMN owner_type TYPE VARCHAR(40);
UPDATE age_groups
SET owner_type = CASE UPPER(owner_type)
                     WHEN 'PROGRAM' THEN 'PROGRAM_TRAINING_APPLICATION'
                     ELSE 'COURSE_TRAINING_APPLICATION'
    END;
ALTER TABLE age_groups ADD CONSTRAINT chk_age_groups_owner_type
    CHECK (UPPER(owner_type) IN ('INSTRUCTOR', 'ORGANISATION', 'COURSE_TRAINING_APPLICATION',
                                 'PROGRAM_TRAINING_APPLICATION'));

ALTER TABLE age_groups RENAME CONSTRAINT chk_training_application_learner_groups_ages TO chk_age_groups_ages;
ALTER TABLE age_groups ADD CONSTRAINT chk_age_groups_max_age CHECK (max_age <= 120);
ALTER TABLE age_groups RENAME CONSTRAINT chk_training_application_learner_groups_name TO chk_age_groups_name;

DROP INDEX uq_training_application_learner_group_name;
CREATE UNIQUE INDEX uq_age_groups_owner_name ON age_groups (UPPER(owner_type), owner_uuid, LOWER(name));
ALTER INDEX idx_training_application_learner_groups_application RENAME TO idx_age_groups_owner;

ALTER TABLE training_application_lesson_hours RENAME TO age_group_lesson_hours;
ALTER SEQUENCE training_application_lesson_hours_id_seq RENAME TO age_group_lesson_hours_id_seq;
ALTER TABLE age_group_lesson_hours RENAME COLUMN learner_group_uuid TO age_group_uuid;
ALTER TABLE age_group_lesson_hours
    RENAME CONSTRAINT training_application_lesson_hours_learner_group_uuid_fkey TO fk_age_group_lesson_hours_age_group;
ALTER TABLE age_group_lesson_hours
    RENAME CONSTRAINT chk_training_application_lesson_hours_hours TO chk_age_group_lesson_hours_hours;
ALTER TABLE age_group_lesson_hours
    RENAME CONSTRAINT uq_training_application_lesson_hours TO uq_age_group_lesson_hours;
