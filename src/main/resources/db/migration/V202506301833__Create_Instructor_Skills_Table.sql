-- V202506301834__Create_proficiency_level_enum.sql
CREATE TYPE proficiency_level_enum AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT');

-- V1.3__Create_instructor_skills_table.sql
CREATE TABLE instructor_skills
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                   NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    instructor_uuid   UUID                   NOT NULL,
    skill_name        VARCHAR(255)           NOT NULL,
    proficiency_level proficiency_level_enum NOT NULL,
    created_date      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP,
    created_by        VARCHAR(255)           NOT NULL,
    updated_by        VARCHAR(255),

    CONSTRAINT fk_instructor_skills_instructor
        FOREIGN KEY (instructor_uuid) REFERENCES instructors (uuid) ON DELETE CASCADE,
    CONSTRAINT unique_instructor_skill UNIQUE (instructor_uuid, skill_name)
);

CREATE INDEX idx_instructor_skills_instructor_id ON instructor_skills(instructor_uuid);
CREATE INDEX idx_instructor_skills_uuid ON instructor_skills(uuid);
CREATE INDEX idx_instructor_skills_proficiency ON instructor_skills(proficiency_level);