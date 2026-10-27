-- Append-only skill evidence from graded work; a regrade adds a superseding row and keeps the old one.
CREATE TABLE IF NOT EXISTS learner_skill_evidence
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    student_uuid           UUID          NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,
    program_uuid           UUID          NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    skill_uuid             UUID          NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    course_enrollment_uuid UUID          NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    course_assessment_uuid UUID          NOT NULL,
    line_item_uuid         UUID          NOT NULL,
    score                  NUMERIC(5, 2) NOT NULL,
    level_name             VARCHAR(60)   NOT NULL,
    counts_as_achieved     BOOLEAN       NOT NULL,
    graded_at              TIMESTAMP     NOT NULL,
    supersedes_uuid        UUID,
    superseded             BOOLEAN       NOT NULL DEFAULT false,
    created_date           TIMESTAMP     NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by             VARCHAR(255)  NOT NULL,
    updated_date           TIMESTAMP,
    updated_by             VARCHAR(255),
    CONSTRAINT chk_learner_skill_evidence_score CHECK (score >= 0 AND score <= 100)
);

CREATE INDEX IF NOT EXISTS idx_learner_skill_evidence_learner
    ON learner_skill_evidence (student_uuid, program_uuid, skill_uuid, graded_at DESC);

-- The learner's current level per programme skill, rebuilt from the latest live evidence.
CREATE TABLE IF NOT EXISTS learner_skill_levels
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    student_uuid       UUID          NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,
    program_uuid       UUID          NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    skill_uuid         UUID          NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    evidence_uuid      UUID          NOT NULL,
    score              NUMERIC(5, 2) NOT NULL,
    level_name         VARCHAR(60)   NOT NULL,
    level_rank         INTEGER       NOT NULL,
    counts_as_achieved BOOLEAN       NOT NULL,
    first_achieved_at  TIMESTAMP,
    graded_at          TIMESTAMP     NOT NULL,
    created_date       TIMESTAMP     NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by         VARCHAR(255)  NOT NULL,
    updated_date       TIMESTAMP,
    updated_by         VARCHAR(255),
    CONSTRAINT uq_learner_skill_levels UNIQUE (student_uuid, program_uuid, skill_uuid)
);
