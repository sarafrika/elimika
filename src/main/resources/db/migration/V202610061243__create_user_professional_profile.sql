-- One user-owned professional profile and skills wallet, shared by every domain a user holds.
-- The instructor_* and course_creator_* qualification tables stay in place as legacy copies.

CREATE TABLE IF NOT EXISTS user_professional_profiles
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid             UUID         NOT NULL UNIQUE REFERENCES users (uuid) ON DELETE CASCADE,
    bio                   TEXT,
    professional_headline VARCHAR(500),
    website               VARCHAR(500),
    location_name         VARCHAR(255),
    lat                   NUMERIC,
    long                  NUMERIC,
    created_date          TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by            VARCHAR(255) NOT NULL,
    updated_date          TIMESTAMP,
    updated_by            VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_skills
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid           UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    skill_name          VARCHAR(255) NOT NULL,
    skill_uuid          UUID REFERENCES skills (uuid) ON DELETE SET NULL,
    proficiency_level   VARCHAR(32)  NOT NULL DEFAULT 'BEGINNER',
    evidence            TEXT,
    last_assessed_on    DATE,
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verified_at         TIMESTAMP,
    verification_notes  TEXT,
    created_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255) NOT NULL,
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_user_skills_proficiency_level
        CHECK (UPPER(proficiency_level) IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_user_skills_verification_status
        CHECK (UPPER(verification_status) IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

-- A user lists a skill once, however it is spelt or spaced.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_skills_user_name
    ON user_skills (user_uuid, LOWER(BTRIM(REGEXP_REPLACE(skill_name, '\s+', ' ', 'g'))));
CREATE INDEX IF NOT EXISTS idx_user_skills_skill_uuid ON user_skills (skill_uuid);

CREATE TABLE IF NOT EXISTS user_education
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid          UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    qualification      VARCHAR(255) NOT NULL,
    field_of_study     VARCHAR(255),
    school_name        VARCHAR(255) NOT NULL,
    start_year         INTEGER,
    year_completed     INTEGER,
    certificate_number VARCHAR(100),
    created_date       TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by         VARCHAR(255) NOT NULL,
    updated_date       TIMESTAMP,
    updated_by         VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_experience
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid           UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    position            VARCHAR(255) NOT NULL,
    organization_name   VARCHAR(255) NOT NULL,
    responsibilities    TEXT,
    years_of_experience NUMERIC,
    start_date          DATE,
    end_date            DATE,
    is_current_position BOOLEAN      NOT NULL DEFAULT FALSE,
    experience_type     VARCHAR(30),
    created_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255) NOT NULL,
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_user_experience_type
        CHECK (experience_type IS NULL OR UPPER(experience_type) IN ('TRAINING', 'WORK', 'VOLUNTEERING', 'PROJECT'))
);

CREATE TABLE IF NOT EXISTS user_memberships
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid         UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    organization_name VARCHAR(255) NOT NULL,
    membership_number VARCHAR(100),
    start_date        DATE,
    end_date          DATE,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date      TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by        VARCHAR(255) NOT NULL,
    updated_date      TIMESTAMP,
    updated_by        VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_certifications
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid            UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    certification_name   VARCHAR(255) NOT NULL,
    issuing_organization VARCHAR(255) NOT NULL,
    issued_date          DATE,
    expiry_date          DATE,
    credential_id        VARCHAR(120),
    credential_url       VARCHAR(500),
    description          TEXT,
    credential_type      VARCHAR(30),
    verification_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verified_at          TIMESTAMP,
    verification_notes   TEXT,
    created_date         TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by           VARCHAR(255) NOT NULL,
    updated_date         TIMESTAMP,
    updated_by           VARCHAR(255),
    CONSTRAINT chk_user_certifications_credential_type
        CHECK (credential_type IS NULL OR UPPER(credential_type) IN ('CERTIFICATE', 'BADGE', 'AWARD', 'EXTERNAL_CREDENTIAL')),
    CONSTRAINT chk_user_certifications_verification_status
        CHECK (UPPER(verification_status) IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS user_portfolio_items
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid    UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    item_type    VARCHAR(30)  NOT NULL,
    link_url     VARCHAR(2048),
    completed_on DATE,
    description  TEXT,
    created_date TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by   VARCHAR(255) NOT NULL,
    updated_date TIMESTAMP,
    updated_by   VARCHAR(255),
    CONSTRAINT chk_user_portfolio_items_type
        CHECK (UPPER(item_type) IN ('PROJECT', 'PERFORMANCE', 'WORK_SAMPLE', 'MEDIA', 'OTHER'))
);

CREATE TABLE IF NOT EXISTS user_competencies
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid           UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    competency          VARCHAR(255) NOT NULL,
    framework           VARCHAR(255),
    level               INTEGER,
    evidence            TEXT,
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verified_at         TIMESTAMP,
    verification_notes  TEXT,
    created_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255) NOT NULL,
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_user_competencies_level CHECK (level IS NULL OR level BETWEEN 1 AND 5),
    CONSTRAINT chk_user_competencies_verification_status
        CHECK (UPPER(verification_status) IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS user_achievements
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid        UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    achievement_type VARCHAR(30)  NOT NULL,
    awarded_by       VARCHAR(255),
    awarded_on       DATE,
    description      TEXT,
    created_date     TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by       VARCHAR(255) NOT NULL,
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_user_achievements_type
        CHECK (UPPER(achievement_type) IN ('AWARD', 'MILESTONE', 'COMPETITION', 'UNLOCKED_SKILL', 'RECOGNITION'))
);

CREATE TABLE IF NOT EXISTS user_documents
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid          UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    document_type_uuid UUID         NOT NULL REFERENCES document_types (uuid),
    education_uuid     UUID REFERENCES user_education (uuid) ON DELETE CASCADE,
    experience_uuid    UUID REFERENCES user_experience (uuid) ON DELETE CASCADE,
    membership_uuid    UUID REFERENCES user_memberships (uuid) ON DELETE CASCADE,
    original_filename  VARCHAR(255) NOT NULL,
    stored_filename    VARCHAR(255) NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    file_size_bytes    BIGINT       NOT NULL,
    mime_type          VARCHAR(100) NOT NULL,
    file_hash          VARCHAR(64),
    title              VARCHAR(255),
    description        TEXT,
    upload_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    is_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_by        VARCHAR(255),
    verified_at        TIMESTAMP,
    verification_notes TEXT,
    status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    expiry_date        DATE,
    created_date       TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by         VARCHAR(255) NOT NULL,
    updated_date       TIMESTAMP,
    updated_by         VARCHAR(255),
    CONSTRAINT chk_user_documents_status
        CHECK (UPPER(status) IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_user_education_user ON user_education (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_experience_user ON user_experience (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_memberships_user ON user_memberships (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_certifications_user ON user_certifications (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_portfolio_items_user ON user_portfolio_items (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_competencies_user ON user_competencies (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_achievements_user ON user_achievements (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_documents_user ON user_documents (user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_documents_pending ON user_documents (is_verified) WHERE is_verified IS FALSE;

-- The domain rows keep a synced copy of the basics, so they must hold what the profile holds.
ALTER TABLE instructors
    ALTER COLUMN professional_headline TYPE VARCHAR(500),
    ALTER COLUMN website TYPE VARCHAR(500);

COMMENT ON TABLE user_professional_profiles IS 'User-owned professional basics shared by every domain; instructors and course_creators keep a synced copy.';
COMMENT ON TABLE user_skills IS 'User-owned skills wallet; verification is per item and holds for every domain.';
COMMENT ON TABLE user_documents IS 'User-owned credential documents; supersedes instructor_documents and course_creator_documents.';
