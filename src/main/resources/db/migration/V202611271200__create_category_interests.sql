-- Course categories a user picks as interests during onboarding, kept per domain (student, instructor,
-- course_creator, organisation_user) because the same account can hold several. Keyed by user rather than
-- by domain profile: the interests step comes before the student, instructor or organisation record exists.
-- Allowed domains live in the API, not in a CHECK constraint, so they can change without a migration.
CREATE TABLE IF NOT EXISTS category_interests
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_uuid     UUID         NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    user_domain   VARCHAR(40)  NOT NULL,
    category_uuid UUID         NOT NULL REFERENCES course_categories (uuid) ON DELETE CASCADE,
    created_date  TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by    VARCHAR(255) NOT NULL,
    updated_date  TIMESTAMP,
    updated_by    VARCHAR(255),
    CONSTRAINT uq_category_interests UNIQUE (user_uuid, user_domain, category_uuid)
);

CREATE INDEX IF NOT EXISTS idx_category_interests_category ON category_interests (category_uuid);

COMMENT ON TABLE category_interests IS 'Course categories a user is interested in, per domain.';
