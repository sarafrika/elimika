-- Skills a marketplace job asks of its instructor, tagged by the posting organisation. Optional: a job
-- with no rows inherits its course's skills (course_skills) when read.

CREATE TABLE class_marketplace_job_required_skills
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    job_uuid        UUID         NOT NULL REFERENCES class_marketplace_jobs (uuid) ON DELETE CASCADE,
    skill_uuid      UUID         NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    min_proficiency VARCHAR(32)  NOT NULL DEFAULT 'BEGINNER',
    is_mandatory    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date    TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by      VARCHAR(255) NOT NULL,
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(255),
    CONSTRAINT uq_class_marketplace_job_required_skills_job_skill UNIQUE (job_uuid, skill_uuid),
    CONSTRAINT chk_class_marketplace_job_required_skills_min_proficiency
        CHECK (UPPER(min_proficiency) IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'))
);

CREATE INDEX idx_class_marketplace_job_required_skills_skill_uuid
    ON class_marketplace_job_required_skills (skill_uuid);
