-- Copies instructor_* and course_creator_* qualifications into the user-owned profile tables.
-- Rows are keyed to the owning profile's user; identical rows (same normalised key) collapse into
-- one, keeping the instructor row's UUID first so existing references keep resolving.

CREATE FUNCTION pg_temp.norm(value TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE AS
$$
SELECT LOWER(BTRIM(REGEXP_REPLACE(COALESCE(value, ''), '\s+', ' ', 'g')))
$$;

-- ---------------------------------------------------------------- profile basics
INSERT INTO user_professional_profiles (user_uuid, bio, professional_headline, website, location_name, lat, long,
                                        created_date, created_by)
SELECT u.uuid,
       COALESCE(CASE WHEN BTRIM(i.bio) <> '' THEN i.bio END, CASE WHEN BTRIM(c.bio) <> '' THEN c.bio END),
       COALESCE(CASE WHEN BTRIM(i.professional_headline) <> '' THEN i.professional_headline END,
                CASE WHEN BTRIM(c.professional_headline) <> '' THEN c.professional_headline END),
       COALESCE(CASE WHEN BTRIM(i.website) <> '' THEN i.website END, CASE WHEN BTRIM(c.website) <> '' THEN c.website END),
       COALESCE(CASE WHEN BTRIM(i.location_name) <> '' THEN i.location_name END,
                CASE WHEN BTRIM(c.location_name) <> '' THEN c.location_name END),
       CASE WHEN i.lat IS NOT NULL AND i.long IS NOT NULL THEN i.lat ELSE c.lat END,
       CASE WHEN i.lat IS NOT NULL AND i.long IS NOT NULL THEN i.long ELSE c.long END,
       (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
       'system'
FROM users u
         LEFT JOIN LATERAL (SELECT x.* FROM instructors x WHERE x.user_uuid = u.uuid
                            ORDER BY x.deleted, x.id DESC LIMIT 1) i ON TRUE
         LEFT JOIN LATERAL (SELECT x.* FROM course_creators x WHERE x.user_uuid = u.uuid
                            ORDER BY x.id DESC LIMIT 1) c ON TRUE
WHERE i.id IS NOT NULL
   OR c.id IS NOT NULL
ON CONFLICT (user_uuid) DO NOTHING;

-- The domain rows hold a synced copy of the basics from here on.
UPDATE instructors i
SET bio                   = p.bio,
    professional_headline = p.professional_headline,
    website               = p.website,
    location_name         = p.location_name,
    lat                   = p.lat,
    long                  = p.long
FROM user_professional_profiles p
WHERE p.user_uuid = i.user_uuid;

UPDATE course_creators c
SET bio                   = p.bio,
    professional_headline = p.professional_headline,
    website               = p.website,
    location_name         = p.location_name,
    lat                   = p.lat,
    long                  = p.long
FROM user_professional_profiles p
WHERE p.user_uuid = c.user_uuid;

-- ---------------------------------------------------------------- skills
CREATE TEMP TABLE bf_skills ON COMMIT DROP AS
SELECT s.*,
       FIRST_VALUE(s.src_uuid) OVER w AS target_uuid,
       ROW_NUMBER() OVER w            AS rn
FROM (SELECT sk.uuid AS src_uuid, i.user_uuid, 1 AS pri, sk.id AS src_id, sk.skill_name, sk.skill_uuid,
             sk.proficiency_level::TEXT AS proficiency_level, NULL::TEXT AS evidence, NULL::DATE AS last_assessed_on,
             'PENDING'::TEXT AS verification_status, NULL::TIMESTAMP AS verified_at, NULL::TEXT AS verification_notes,
             sk.created_date, sk.created_by, sk.updated_date, sk.updated_by
      FROM instructor_skills sk
               JOIN instructors i ON i.uuid = sk.instructor_uuid
               JOIN users u ON u.uuid = i.user_uuid
      UNION ALL
      SELECT sk.uuid, c.user_uuid, 2, sk.id, sk.skill_name, sk.skill_uuid,
             sk.proficiency_level::TEXT, sk.evidence, sk.last_assessed_on,
             sk.verification_status::TEXT, sk.verified_at, sk.verification_notes,
             sk.created_date AT TIME ZONE 'UTC', sk.created_by, sk.updated_date AT TIME ZONE 'UTC', sk.updated_by
      FROM course_creator_skills sk
               JOIN course_creators c ON c.uuid = sk.course_creator_uuid
               JOIN users u ON u.uuid = c.user_uuid) s
WHERE pg_temp.norm(s.skill_name) <> ''
WINDOW w AS (PARTITION BY s.user_uuid, pg_temp.norm(s.skill_name) ORDER BY s.pri, s.src_id);

INSERT INTO user_skills (uuid, user_uuid, skill_name, skill_uuid, proficiency_level, evidence, last_assessed_on,
                         verification_status, verified_at, verification_notes, created_date, created_by,
                         updated_date, updated_by)
SELECT src_uuid, user_uuid, BTRIM(skill_name), skill_uuid, UPPER(COALESCE(proficiency_level, 'BEGINNER')), evidence,
       last_assessed_on, UPPER(COALESCE(verification_status, 'PENDING')), verified_at, verification_notes,
       COALESCE(created_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE(created_by, 'system'),
       updated_date, updated_by
FROM bf_skills
WHERE rn = 1;

-- A collapsed duplicate may carry the evidence, taxonomy link or verdict the survivor lacks.
UPDATE user_skills us
SET skill_uuid          = COALESCE(us.skill_uuid, d.skill_uuid),
    evidence            = COALESCE(us.evidence, d.evidence),
    last_assessed_on    = COALESCE(us.last_assessed_on, d.last_assessed_on),
    verification_status = CASE WHEN UPPER(us.verification_status) = 'PENDING'
                                   THEN UPPER(d.verification_status) ELSE us.verification_status END,
    verified_at         = CASE WHEN UPPER(us.verification_status) = 'PENDING'
                                   THEN d.verified_at ELSE us.verified_at END,
    verification_notes  = CASE WHEN UPPER(us.verification_status) = 'PENDING'
                                   THEN d.verification_notes ELSE us.verification_notes END
FROM (SELECT DISTINCT ON (target_uuid) target_uuid, skill_uuid, evidence, last_assessed_on,
                                       COALESCE(verification_status, 'PENDING') AS verification_status,
                                       verified_at, verification_notes
      FROM bf_skills
      WHERE rn > 1
      ORDER BY target_uuid,
               CASE UPPER(verification_status) WHEN 'VERIFIED' THEN 0 WHEN 'REJECTED' THEN 1 ELSE 2 END,
               src_id) d
WHERE us.uuid = d.target_uuid;

-- ---------------------------------------------------------------- education
CREATE TEMP TABLE bf_education ON COMMIT DROP AS
SELECT s.*,
       FIRST_VALUE(s.src_uuid) OVER w AS target_uuid,
       ROW_NUMBER() OVER w            AS rn
FROM (SELECT e.uuid AS src_uuid, i.user_uuid, 1 AS pri, e.id AS src_id, e.qualification, e.field_of_study,
             e.school_name, e.start_year, e.year_completed, e.certificate_number,
             e.created_date, e.created_by, e.updated_date, e.updated_by
      FROM instructor_education e
               JOIN instructors i ON i.uuid = e.instructor_uuid
               JOIN users u ON u.uuid = i.user_uuid
      UNION ALL
      SELECT e.uuid, c.user_uuid, 2, e.id, e.qualification, e.field_of_study,
             e.school_name, e.start_year, e.year_completed, e.certificate_number,
             e.created_date AT TIME ZONE 'UTC', e.created_by, e.updated_date AT TIME ZONE 'UTC', e.updated_by
      FROM course_creator_education e
               JOIN course_creators c ON c.uuid = e.course_creator_uuid
               JOIN users u ON u.uuid = c.user_uuid) s
WINDOW w AS (PARTITION BY s.user_uuid, pg_temp.norm(s.qualification), pg_temp.norm(s.school_name),
        COALESCE(s.year_completed, -1) ORDER BY s.pri, s.src_id);

INSERT INTO user_education (uuid, user_uuid, qualification, field_of_study, school_name, start_year, year_completed,
                            certificate_number, created_date, created_by, updated_date, updated_by)
SELECT src_uuid, user_uuid, qualification, field_of_study, school_name, start_year, year_completed,
       certificate_number, COALESCE(created_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
       COALESCE(created_by, 'system'), updated_date, updated_by
FROM bf_education
WHERE rn = 1;

UPDATE user_education ue
SET field_of_study     = COALESCE(ue.field_of_study, d.field_of_study),
    start_year         = COALESCE(ue.start_year, d.start_year),
    certificate_number = COALESCE(ue.certificate_number, d.certificate_number)
FROM (SELECT DISTINCT ON (target_uuid) target_uuid, field_of_study, start_year, certificate_number
      FROM bf_education
      WHERE rn > 1
      ORDER BY target_uuid, src_id) d
WHERE ue.uuid = d.target_uuid;

-- ---------------------------------------------------------------- experience
CREATE TEMP TABLE bf_experience ON COMMIT DROP AS
SELECT s.*,
       FIRST_VALUE(s.src_uuid) OVER w AS target_uuid,
       ROW_NUMBER() OVER w            AS rn
FROM (SELECT x.uuid AS src_uuid, i.user_uuid, 1 AS pri, x.id AS src_id, x.position, x.organization_name,
             x.responsibilities, x.years_of_experience, x.start_date, x.end_date, x.is_current_position,
             NULL::TEXT AS experience_type, x.created_date, x.created_by, x.updated_date, x.updated_by
      FROM instructor_experience x
               JOIN instructors i ON i.uuid = x.instructor_uuid
               JOIN users u ON u.uuid = i.user_uuid
      UNION ALL
      SELECT x.uuid, c.user_uuid, 2, x.id, x.position, x.organization_name,
             x.responsibilities, x.years_of_experience, x.start_date, x.end_date, x.is_current_position,
             x.experience_type::TEXT, x.created_date AT TIME ZONE 'UTC', x.created_by,
             x.updated_date AT TIME ZONE 'UTC', x.updated_by
      FROM course_creator_experience x
               JOIN course_creators c ON c.uuid = x.course_creator_uuid
               JOIN users u ON u.uuid = c.user_uuid) s
WINDOW w AS (PARTITION BY s.user_uuid, pg_temp.norm(s.position), pg_temp.norm(s.organization_name),
        COALESCE(s.start_date, DATE '1900-01-01') ORDER BY s.pri, s.src_id);

INSERT INTO user_experience (uuid, user_uuid, position, organization_name, responsibilities, years_of_experience,
                             start_date, end_date, is_current_position, experience_type, created_date, created_by,
                             updated_date, updated_by)
SELECT src_uuid, user_uuid, position, organization_name, responsibilities, years_of_experience,
       start_date, end_date, COALESCE(is_current_position, FALSE), UPPER(experience_type),
       COALESCE(created_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE(created_by, 'system'),
       updated_date, updated_by
FROM bf_experience
WHERE rn = 1;

UPDATE user_experience ue
SET experience_type     = COALESCE(ue.experience_type, d.experience_type),
    responsibilities    = COALESCE(ue.responsibilities, d.responsibilities),
    years_of_experience = COALESCE(ue.years_of_experience, d.years_of_experience),
    end_date            = COALESCE(ue.end_date, d.end_date)
FROM (SELECT DISTINCT ON (target_uuid) target_uuid, UPPER(experience_type) AS experience_type, responsibilities,
                                       years_of_experience, end_date
      FROM bf_experience
      WHERE rn > 1
      ORDER BY target_uuid, src_id) d
WHERE ue.uuid = d.target_uuid;

-- ---------------------------------------------------------------- memberships
CREATE TEMP TABLE bf_memberships ON COMMIT DROP AS
SELECT s.*,
       FIRST_VALUE(s.src_uuid) OVER w AS target_uuid,
       ROW_NUMBER() OVER w            AS rn
FROM (SELECT m.uuid AS src_uuid, i.user_uuid, 1 AS pri, m.id AS src_id, m.organization_name, m.membership_number,
             m.start_date, m.end_date, m.is_active, m.created_date, m.created_by, m.updated_date, m.updated_by
      FROM instructor_professional_memberships m
               JOIN instructors i ON i.uuid = m.instructor_uuid
               JOIN users u ON u.uuid = i.user_uuid
      UNION ALL
      SELECT m.uuid, c.user_uuid, 2, m.id, m.organization_name, m.membership_number,
             m.start_date, m.end_date, m.is_active, m.created_date AT TIME ZONE 'UTC', m.created_by,
             m.updated_date AT TIME ZONE 'UTC', m.updated_by
      FROM course_creator_professional_memberships m
               JOIN course_creators c ON c.uuid = m.course_creator_uuid
               JOIN users u ON u.uuid = c.user_uuid) s
WINDOW w AS (PARTITION BY s.user_uuid, pg_temp.norm(s.organization_name), pg_temp.norm(s.membership_number)
        ORDER BY s.pri, s.src_id);

INSERT INTO user_memberships (uuid, user_uuid, organization_name, membership_number, start_date, end_date, is_active,
                              created_date, created_by, updated_date, updated_by)
SELECT src_uuid, user_uuid, organization_name, membership_number, start_date, end_date, COALESCE(is_active, TRUE),
       COALESCE(created_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE(created_by, 'system'),
       updated_date, updated_by
FROM bf_memberships
WHERE rn = 1;

-- ---------------------------------------------------------------- certifications (course creators only)
CREATE TEMP TABLE bf_certifications ON COMMIT DROP AS
SELECT s.*, ROW_NUMBER() OVER w AS rn
FROM (SELECT x.uuid AS src_uuid, c.user_uuid, x.id AS src_id, x.certification_name, x.issuing_organization,
             x.issued_date, x.expiry_date, x.credential_id, x.credential_url, x.description,
             x.credential_type::TEXT AS credential_type, COALESCE(x.is_verified, FALSE) AS is_verified,
             x.created_date AT TIME ZONE 'UTC' AS created_date, x.created_by,
             x.updated_date AT TIME ZONE 'UTC' AS updated_date, x.updated_by
      FROM course_creator_certifications x
               JOIN course_creators c ON c.uuid = x.course_creator_uuid
               JOIN users u ON u.uuid = c.user_uuid) s
WINDOW w AS (PARTITION BY s.user_uuid, pg_temp.norm(s.certification_name), pg_temp.norm(s.issuing_organization),
        pg_temp.norm(s.credential_id) ORDER BY s.is_verified DESC, s.src_id);

INSERT INTO user_certifications (uuid, user_uuid, certification_name, issuing_organization, issued_date, expiry_date,
                                 credential_id, credential_url, description, credential_type, verification_status,
                                 verified_at, created_date, created_by, updated_date, updated_by)
SELECT src_uuid, user_uuid, certification_name, issuing_organization, issued_date, expiry_date,
       credential_id, credential_url, description, UPPER(credential_type),
       CASE WHEN is_verified THEN 'VERIFIED' ELSE 'PENDING' END,
       CASE WHEN is_verified THEN COALESCE(updated_date, created_date) END,
       COALESCE(created_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE(created_by, 'system'),
       updated_date, updated_by
FROM bf_certifications
WHERE rn = 1;

-- ---------------------------------------------------------------- portfolio, competencies, achievements
INSERT INTO user_portfolio_items (uuid, user_uuid, title, item_type, link_url, completed_on, description,
                                  created_date, created_by, updated_date, updated_by)
SELECT DISTINCT ON (c.user_uuid, pg_temp.norm(p.title), UPPER(p.item_type))
       p.uuid, c.user_uuid, p.title, UPPER(p.item_type), p.link_url, p.completed_on, p.description,
       p.created_date, p.created_by, p.updated_date, p.updated_by
FROM course_creator_portfolio_items p
         JOIN course_creators c ON c.uuid = p.course_creator_uuid
         JOIN users u ON u.uuid = c.user_uuid
ORDER BY c.user_uuid, pg_temp.norm(p.title), UPPER(p.item_type), p.id;

INSERT INTO user_competencies (uuid, user_uuid, competency, framework, level, evidence, verification_status,
                               verified_at, verification_notes, created_date, created_by, updated_date, updated_by)
SELECT DISTINCT ON (c.user_uuid, pg_temp.norm(x.competency), pg_temp.norm(x.framework))
       x.uuid, c.user_uuid, x.competency, x.framework, x.level, x.evidence, UPPER(x.verification_status),
       x.verified_at, x.verification_notes, x.created_date, x.created_by, x.updated_date, x.updated_by
FROM course_creator_competencies x
         JOIN course_creators c ON c.uuid = x.course_creator_uuid
         JOIN users u ON u.uuid = c.user_uuid
ORDER BY c.user_uuid, pg_temp.norm(x.competency), pg_temp.norm(x.framework),
         CASE UPPER(x.verification_status) WHEN 'VERIFIED' THEN 0 WHEN 'REJECTED' THEN 1 ELSE 2 END, x.id;

INSERT INTO user_achievements (uuid, user_uuid, title, achievement_type, awarded_by, awarded_on, description,
                               created_date, created_by, updated_date, updated_by)
SELECT DISTINCT ON (c.user_uuid, pg_temp.norm(a.title), a.awarded_on)
       a.uuid, c.user_uuid, a.title, UPPER(a.achievement_type), a.awarded_by, a.awarded_on, a.description,
       a.created_date, a.created_by, a.updated_date, a.updated_by
FROM course_creator_achievements a
         JOIN course_creators c ON c.uuid = a.course_creator_uuid
         JOIN users u ON u.uuid = c.user_uuid
ORDER BY c.user_uuid, pg_temp.norm(a.title), a.awarded_on, a.id;

-- ---------------------------------------------------------------- documents
-- Links to education, experience and membership rows follow the row they collapsed into.
INSERT INTO user_documents (uuid, user_uuid, document_type_uuid, education_uuid, experience_uuid, membership_uuid,
                            original_filename, stored_filename, file_path, file_size_bytes, mime_type, file_hash,
                            title, description, upload_date, is_verified, verified_by, verified_at,
                            verification_notes, status, expiry_date, created_date, created_by, updated_date,
                            updated_by)
SELECT DISTINCT ON (d.user_uuid, d.file_path)
       d.src_uuid, d.user_uuid, d.document_type_uuid,
       (SELECT b.target_uuid FROM bf_education b WHERE b.src_uuid = d.education_uuid),
       (SELECT b.target_uuid FROM bf_experience b WHERE b.src_uuid = d.experience_uuid),
       (SELECT b.target_uuid FROM bf_memberships b WHERE b.src_uuid = d.membership_uuid),
       d.original_filename, d.stored_filename, d.file_path, d.file_size_bytes, d.mime_type, d.file_hash,
       d.title, d.description, COALESCE(d.upload_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
       COALESCE(d.is_verified, FALSE), d.verified_by, d.verified_at, d.verification_notes,
       UPPER(COALESCE(d.status, 'PENDING')), d.expiry_date,
       COALESCE(d.created_date, CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), COALESCE(d.created_by, 'system'),
       d.updated_date, d.updated_by
FROM (SELECT x.uuid AS src_uuid, i.user_uuid, 1 AS pri, x.id AS src_id, x.document_type_uuid, x.education_uuid,
             x.experience_uuid, x.membership_uuid, x.original_filename, x.stored_filename, x.file_path,
             x.file_size_bytes, x.mime_type, x.file_hash, x.title, x.description, x.upload_date, x.is_verified,
             x.verified_by, x.verified_at, x.verification_notes, x.status::TEXT AS status, x.expiry_date,
             x.created_date, x.created_by, x.updated_date, x.updated_by
      FROM instructor_documents x
               JOIN instructors i ON i.uuid = x.instructor_uuid
               JOIN users u ON u.uuid = i.user_uuid
      UNION ALL
      SELECT x.uuid, c.user_uuid, 2, x.id, x.document_type_uuid, x.education_uuid,
             x.experience_uuid, x.membership_uuid, x.original_filename, x.stored_filename, x.file_path,
             x.file_size_bytes, x.mime_type, x.file_hash, x.title, x.description, x.upload_date AT TIME ZONE 'UTC',
             x.is_verified, x.verified_by, x.verified_at AT TIME ZONE 'UTC', x.verification_notes, x.status::TEXT,
             x.expiry_date, x.created_date AT TIME ZONE 'UTC', x.created_by, x.updated_date AT TIME ZONE 'UTC',
             x.updated_by
      FROM course_creator_documents x
               JOIN course_creators c ON c.uuid = x.course_creator_uuid
               JOIN users u ON u.uuid = c.user_uuid) d
ORDER BY d.user_uuid, d.file_path, COALESCE(d.is_verified, FALSE) DESC, d.pri, d.src_id;

-- New user documents register under their own owner type; the backfilled ones keep theirs.
INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT d.file_path, d.original_filename, 'USER_DOCUMENT', d.user_uuid, 'system'
FROM user_documents d
WHERE NOT EXISTS (SELECT 1 FROM media_files m WHERE m.file_key = d.file_path);
