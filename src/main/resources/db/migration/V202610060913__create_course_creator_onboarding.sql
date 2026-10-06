ALTER TABLE course_creators
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS verification_requested_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS review_reason TEXT;

UPDATE course_creators
SET verification_status = CASE WHEN admin_verified IS TRUE THEN 'APPROVED' ELSE 'DRAFT' END
WHERE verification_status IS NULL;

ALTER TABLE course_creators
    ALTER COLUMN verification_status SET DEFAULT 'DRAFT',
    ALTER COLUMN verification_status SET NOT NULL;

ALTER TABLE course_creators
    DROP CONSTRAINT IF EXISTS chk_course_creators_verification_status;

ALTER TABLE course_creators
    ADD CONSTRAINT chk_course_creators_verification_status
        CHECK (UPPER(verification_status) IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'REVOKED'));

CREATE INDEX IF NOT EXISTS idx_course_creators_verification_status
    ON course_creators (verification_status);

CREATE INDEX IF NOT EXISTS idx_course_creators_submitted_at
    ON course_creators (submitted_at);

ALTER TABLE course_creator_skills
    ADD COLUMN IF NOT EXISTS skill_uuid UUID;

ALTER TABLE course_creator_skills
    DROP CONSTRAINT IF EXISTS fk_course_creator_skills_skill;

ALTER TABLE course_creator_skills
    ADD CONSTRAINT fk_course_creator_skills_skill
        FOREIGN KEY (skill_uuid) REFERENCES skills (uuid) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_course_creator_skills_skill_uuid
    ON course_creator_skills (skill_uuid);

CREATE TABLE IF NOT EXISTS course_creator_category_preferences
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    course_creator_uuid UUID         NOT NULL REFERENCES course_creators (uuid) ON DELETE CASCADE,
    category_uuid       UUID         NOT NULL REFERENCES course_categories (uuid) ON DELETE CASCADE,
    created_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255) NOT NULL,
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(255),
    CONSTRAINT uq_course_creator_category_preferences UNIQUE (course_creator_uuid, category_uuid)
);

CREATE INDEX IF NOT EXISTS idx_course_creator_category_preferences_creator
    ON course_creator_category_preferences (course_creator_uuid);

CREATE INDEX IF NOT EXISTS idx_course_creator_category_preferences_category
    ON course_creator_category_preferences (category_uuid);

COMMENT ON COLUMN course_creators.verification_status IS 'Course creator onboarding review lifecycle.';
COMMENT ON TABLE course_creator_category_preferences IS 'Course categories a course creator selected during onboarding.';

-- Skills wallet: evidence and admin verification on skills, types on credentials and experience,
-- and the portfolio, competency and achievement sections.
ALTER TABLE course_creator_skills
    ADD COLUMN IF NOT EXISTS evidence            TEXT,
    ADD COLUMN IF NOT EXISTS last_assessed_on    DATE,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS verified_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verification_notes  TEXT;

ALTER TABLE course_creator_skills
    DROP CONSTRAINT IF EXISTS chk_course_creator_skills_verification_status;
ALTER TABLE course_creator_skills
    ADD CONSTRAINT chk_course_creator_skills_verification_status
        CHECK (UPPER(verification_status) IN ('PENDING', 'VERIFIED', 'REJECTED'));

ALTER TABLE course_creator_certifications
    ADD COLUMN IF NOT EXISTS credential_type VARCHAR(30);
ALTER TABLE course_creator_certifications
    DROP CONSTRAINT IF EXISTS chk_course_creator_certifications_credential_type;
ALTER TABLE course_creator_certifications
    ADD CONSTRAINT chk_course_creator_certifications_credential_type
        CHECK (credential_type IS NULL OR UPPER(credential_type) IN ('CERTIFICATE', 'BADGE', 'AWARD', 'EXTERNAL_CREDENTIAL'));

ALTER TABLE course_creator_experience
    ADD COLUMN IF NOT EXISTS experience_type VARCHAR(30);
ALTER TABLE course_creator_experience
    DROP CONSTRAINT IF EXISTS chk_course_creator_experience_type;
ALTER TABLE course_creator_experience
    ADD CONSTRAINT chk_course_creator_experience_type
        CHECK (experience_type IS NULL OR UPPER(experience_type) IN ('TRAINING', 'WORK', 'VOLUNTEERING', 'PROJECT'));

CREATE TABLE IF NOT EXISTS course_creator_portfolio_items
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    course_creator_uuid UUID         NOT NULL REFERENCES course_creators (uuid) ON DELETE CASCADE,
    title               VARCHAR(255) NOT NULL,
    item_type           VARCHAR(30)  NOT NULL,
    link_url            VARCHAR(2048),
    completed_on        DATE,
    description         TEXT,
    created_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255) NOT NULL,
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_course_creator_portfolio_items_type
        CHECK (UPPER(item_type) IN ('PROJECT', 'PERFORMANCE', 'WORK_SAMPLE', 'MEDIA', 'OTHER'))
);

CREATE TABLE IF NOT EXISTS course_creator_competencies
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    course_creator_uuid UUID         NOT NULL REFERENCES course_creators (uuid) ON DELETE CASCADE,
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
    CONSTRAINT chk_course_creator_competencies_level CHECK (level IS NULL OR level BETWEEN 1 AND 5),
    CONSTRAINT chk_course_creator_competencies_verification_status
        CHECK (UPPER(verification_status) IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS course_creator_achievements
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    course_creator_uuid UUID         NOT NULL REFERENCES course_creators (uuid) ON DELETE CASCADE,
    title               VARCHAR(255) NOT NULL,
    achievement_type    VARCHAR(30)  NOT NULL,
    awarded_by          VARCHAR(255),
    awarded_on          DATE,
    description         TEXT,
    created_date        TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255) NOT NULL,
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(255),
    CONSTRAINT chk_course_creator_achievements_type
        CHECK (UPPER(achievement_type) IN ('AWARD', 'MILESTONE', 'COMPETITION', 'UNLOCKED_SKILL', 'RECOGNITION'))
);

CREATE INDEX IF NOT EXISTS idx_course_creator_portfolio_items_creator ON course_creator_portfolio_items (course_creator_uuid);
CREATE INDEX IF NOT EXISTS idx_course_creator_competencies_creator ON course_creator_competencies (course_creator_uuid);
CREATE INDEX IF NOT EXISTS idx_course_creator_achievements_creator ON course_creator_achievements (course_creator_uuid);
