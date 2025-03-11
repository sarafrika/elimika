-- Create Skills Table
CREATE TABLE IF NOT EXISTS skills
(
    id   BIGSERIAL PRIMARY KEY,
    uuid UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_skills_uuid ON skills (uuid);

-- Create User Skills Table
CREATE TABLE IF NOT EXISTS user_skills
(
    user_uuid  UUID NOT NULL,
    skill_uuid UUID NOT NULL
);

CREATE INDEX idx_user_skills_user_uuid ON user_skills (user_uuid);
CREATE INDEX idx_user_skills_skill_uuid ON user_skills (skill_uuid);