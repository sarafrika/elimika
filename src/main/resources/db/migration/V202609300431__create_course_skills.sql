-- Skills a course teaches, tagged by its creator from the skills taxonomy. Optional: a course with no
-- rows simply has no tags, and tags never gate publishing.

CREATE TABLE course_skills
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    course_uuid  UUID         NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    skill_uuid   UUID         NOT NULL REFERENCES skills (uuid) ON DELETE CASCADE,
    level        VARCHAR(32)  NOT NULL DEFAULT 'BEGINNER',
    weight       INTEGER      NOT NULL DEFAULT 1,
    created_date TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by   VARCHAR(255) NOT NULL,
    updated_date TIMESTAMP,
    updated_by   VARCHAR(255),
    CONSTRAINT uq_course_skills_course_skill UNIQUE (course_uuid, skill_uuid),
    CONSTRAINT chk_course_skills_level CHECK (UPPER(level) IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_course_skills_weight CHECK (weight BETWEEN 1 AND 5)
);

CREATE INDEX idx_course_skills_skill_uuid ON course_skills (skill_uuid);
