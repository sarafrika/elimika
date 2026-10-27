-- Skills a programme promises, and which graded rubric criteria (or whole scores) show each one.
CREATE TABLE IF NOT EXISTS program_target_skills
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    program_uuid  UUID         NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    skill_uuid    UUID         NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    created_date  TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by    VARCHAR(255) NOT NULL,
    updated_date  TIMESTAMP,
    updated_by    VARCHAR(255),
    CONSTRAINT uq_program_target_skills UNIQUE (program_uuid, skill_uuid)
);

CREATE TABLE IF NOT EXISTS program_skill_mappings
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    program_uuid           UUID          NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    skill_uuid             UUID          NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    course_assessment_uuid UUID          NOT NULL REFERENCES course_assessments (uuid) ON DELETE CASCADE,
    rubric_criteria_uuid   UUID REFERENCES rubric_criteria (uuid) ON DELETE CASCADE,
    share_percentage       NUMERIC(5, 2) NOT NULL,
    active                 BOOLEAN       NOT NULL DEFAULT true,
    created_date           TIMESTAMP     NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by             VARCHAR(255)  NOT NULL,
    updated_date           TIMESTAMP,
    updated_by             VARCHAR(255),
    CONSTRAINT chk_program_skill_mappings_share CHECK (share_percentage > 0 AND share_percentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_program_skill_mappings_assessment ON program_skill_mappings (course_assessment_uuid);
CREATE INDEX IF NOT EXISTS idx_program_skill_mappings_program_skill ON program_skill_mappings (program_uuid, skill_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uq_program_skill_mappings_source
    ON program_skill_mappings (program_uuid, skill_uuid, course_assessment_uuid,
                               COALESCE(rubric_criteria_uuid, '00000000-0000-0000-0000-000000000000'::uuid));
