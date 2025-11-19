-- Capture qualification metadata for course creators
CREATE TABLE course_creator_skills
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_creator_uuid UUID                     NOT NULL,
    skill_name          VARCHAR(255)             NOT NULL,
    proficiency_level   proficiency_level_enum   NOT NULL,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date        TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT fk_course_creator_skills_creator FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE CASCADE,
    CONSTRAINT uq_course_creator_skill UNIQUE (course_creator_uuid, skill_name)
);

CREATE INDEX idx_course_creator_skills_uuid ON course_creator_skills (uuid);
CREATE INDEX idx_course_creator_skills_creator_uuid ON course_creator_skills (course_creator_uuid);

CREATE TABLE course_creator_education
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_creator_uuid UUID                     NOT NULL,
    qualification       VARCHAR(255)             NOT NULL,
    school_name         VARCHAR(255)             NOT NULL,
    year_completed      INTEGER,
    certificate_number  VARCHAR(100),
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date        TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT fk_course_creator_education_creator FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_course_creator_education_uuid ON course_creator_education (uuid);
CREATE INDEX idx_course_creator_education_creator_uuid ON course_creator_education (course_creator_uuid);

CREATE TABLE course_creator_experience
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_creator_uuid  UUID                     NOT NULL,
    position             VARCHAR(255)             NOT NULL,
    organization_name    VARCHAR(255)             NOT NULL,
    responsibilities     TEXT,
    years_of_experience  NUMERIC(5,2),
    start_date           DATE,
    end_date             DATE,
    is_current_position  BOOLEAN,
    created_date         TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date         TIMESTAMP WITH TIME ZONE,
    created_by           VARCHAR(255)             NOT NULL,
    updated_by           VARCHAR(255),
    CONSTRAINT fk_course_creator_experience_creator FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_course_creator_experience_uuid ON course_creator_experience (uuid);
CREATE INDEX idx_course_creator_experience_creator_uuid ON course_creator_experience (course_creator_uuid);

CREATE TABLE course_creator_professional_memberships
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_creator_uuid UUID                     NOT NULL,
    organization_name   VARCHAR(255)             NOT NULL,
    membership_number   VARCHAR(100),
    start_date          DATE,
    end_date            DATE,
    is_active           BOOLEAN                  NOT NULL        DEFAULT true,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date        TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT fk_course_creator_memberships_creator FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_course_creator_memberships_uuid ON course_creator_professional_memberships (uuid);
CREATE INDEX idx_course_creator_memberships_creator_uuid ON course_creator_professional_memberships (course_creator_uuid);

CREATE TABLE course_creator_certifications
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_creator_uuid  UUID                     NOT NULL,
    certification_name   VARCHAR(255)             NOT NULL,
    issuing_organization VARCHAR(255)             NOT NULL,
    issued_date          DATE,
    expiry_date          DATE,
    credential_id        VARCHAR(120),
    credential_url       VARCHAR(500),
    description          TEXT,
    is_verified          BOOLEAN,
    created_date         TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date         TIMESTAMP WITH TIME ZONE,
    created_by           VARCHAR(255)             NOT NULL,
    updated_by           VARCHAR(255),
    CONSTRAINT fk_course_creator_certifications_creator FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_course_creator_certifications_uuid ON course_creator_certifications (uuid);
CREATE INDEX idx_course_creator_certifications_creator_uuid ON course_creator_certifications (course_creator_uuid);
