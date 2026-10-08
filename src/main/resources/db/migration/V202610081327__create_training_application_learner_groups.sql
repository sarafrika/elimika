-- An instructor's learner groups for a course or program application: named age bands, each with its
-- own hours for every lesson. Deleting a group deletes its lesson hours.

CREATE TABLE training_application_learner_groups
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_type VARCHAR(16)  NOT NULL,
    application_uuid UUID         NOT NULL,
    name             VARCHAR(80)  NOT NULL,
    min_age          INTEGER      NOT NULL,
    max_age          INTEGER      NOT NULL,
    position         INTEGER      NOT NULL DEFAULT 0,
    created_date     TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by       VARCHAR(255) NOT NULL,
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_training_application_learner_groups_application_type
        CHECK (UPPER(application_type) IN ('COURSE', 'PROGRAM')),
    CONSTRAINT chk_training_application_learner_groups_ages
        CHECK (min_age >= 0 AND min_age <= max_age),
    CONSTRAINT chk_training_application_learner_groups_name
        CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE UNIQUE INDEX uq_training_application_learner_group_name
    ON training_application_learner_groups (UPPER(application_type), application_uuid, LOWER(name));

CREATE INDEX idx_training_application_learner_groups_application
    ON training_application_learner_groups (application_uuid);

CREATE TABLE training_application_lesson_hours
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    learner_group_uuid UUID          NOT NULL
        REFERENCES training_application_learner_groups (uuid) ON DELETE CASCADE,
    lesson_uuid        UUID          NOT NULL,
    hours              NUMERIC(5, 2) NOT NULL,
    created_date       TIMESTAMP     NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by         VARCHAR(255)  NOT NULL,
    updated_date       TIMESTAMP,
    updated_by         VARCHAR(255),
    CONSTRAINT chk_training_application_lesson_hours_hours CHECK (hours > 0 AND hours <= 24),
    CONSTRAINT uq_training_application_lesson_hours UNIQUE (learner_group_uuid, lesson_uuid)
);
