-- Skills a learner has declared they want to learn, picked from the skills taxonomy. Course
-- recommendations read them to find courses covering the learner's skill gap. Owned by the student
-- module; written only by the learner (source SELF) for now.

CREATE TABLE learner_skill_goals
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    student_uuid UUID         NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,
    skill_uuid   UUID         NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    source       VARCHAR(32)  NOT NULL DEFAULT 'SELF',
    created_date TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by   VARCHAR(255) NOT NULL,
    updated_date TIMESTAMP,
    updated_by   VARCHAR(255),
    CONSTRAINT uq_learner_skill_goals_student_skill UNIQUE (student_uuid, skill_uuid),
    CONSTRAINT chk_learner_skill_goals_source CHECK (UPPER(source) IN ('SELF', 'GUARDIAN', 'ADMIN'))
);

CREATE INDEX idx_learner_skill_goals_skill_uuid ON learner_skill_goals (skill_uuid);
