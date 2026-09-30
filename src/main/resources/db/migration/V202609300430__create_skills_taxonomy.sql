-- The admin-curated skills taxonomy, seeded from the skill names instructors and course creators
-- already use, and linked back to instructor_skills by slug.
--
-- The slug rule is SkillSlugs.slugify: lower-case, every run outside [a-z0-9] becomes one hyphen,
-- hyphens trimmed at both ends. Keep the expression below and the Java rule identical.

CREATE TABLE skills
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name         VARCHAR(255) NOT NULL,
    slug         VARCHAR(255) NOT NULL,
    parent_uuid  UUID REFERENCES skills (uuid) ON DELETE SET NULL,
    aliases      TEXT[]       NOT NULL DEFAULT '{}',
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by   VARCHAR(255) NOT NULL,
    updated_date TIMESTAMP,
    updated_by   VARCHAR(255),
    CONSTRAINT uq_skills_slug UNIQUE (slug),
    CONSTRAINT chk_skills_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT chk_skills_not_own_parent CHECK (parent_uuid IS NULL OR parent_uuid <> uuid)
);

CREATE INDEX idx_skills_parent_uuid ON skills (parent_uuid);

-- Starter taxonomy: one skill per distinct slug among the names in use. When several spellings share
-- a slug ("Java", "java", "JAVA "), the most used spelling names the skill, ties broken alphabetically.
WITH used AS (SELECT REGEXP_REPLACE(TRIM(skill_name), '\s+', ' ', 'g') AS name
              FROM instructor_skills
              WHERE skill_name IS NOT NULL
              UNION ALL
              SELECT REGEXP_REPLACE(TRIM(skill_name), '\s+', ' ', 'g')
              FROM course_creator_skills
              WHERE skill_name IS NOT NULL),
     slugged AS (SELECT name, TRIM(BOTH '-' FROM REGEXP_REPLACE(LOWER(name), '[^a-z0-9]+', '-', 'g')) AS slug
                 FROM used),
     counted AS (SELECT slug, name, COUNT(*) AS uses
                 FROM slugged
                 WHERE slug <> ''
                 GROUP BY slug, name)
INSERT
INTO skills (name, slug, created_by)
SELECT DISTINCT ON (slug) name, slug, 'system'
FROM counted
ORDER BY slug, uses DESC, name;

-- Instructor skills point at the taxonomy when their free text resolves; the text itself stays.
ALTER TABLE instructor_skills
    ADD COLUMN skill_uuid UUID REFERENCES skills (uuid) ON DELETE SET NULL;

UPDATE instructor_skills i
SET skill_uuid = s.uuid
FROM skills s
WHERE s.slug = TRIM(BOTH '-' FROM REGEXP_REPLACE(LOWER(TRIM(i.skill_name)), '[^a-z0-9]+', '-', 'g'));

CREATE INDEX idx_instructor_skills_skill_uuid ON instructor_skills (skill_uuid);
