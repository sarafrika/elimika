-- Backfill missing standalone role profiles for users who already carry the matching domain.

INSERT INTO students (user_uuid, full_name, created_by)
SELECT
    u.uuid,
    COALESCE(
        NULLIF(
            TRIM(CONCAT_WS(' ',
                NULLIF(TRIM(u.first_name), ''),
                NULLIF(TRIM(u.middle_name), ''),
                NULLIF(TRIM(u.last_name), '')
            )),
            ''
        ),
        'Unknown'
    ) AS full_name,
    'system'
FROM users u
JOIN user_domain_mapping udm
    ON udm.user_uuid = u.uuid
JOIN user_domain ud
    ON ud.uuid = udm.domain_uuid
LEFT JOIN students s
    ON s.user_uuid = u.uuid
WHERE lower(ud.domain_name) = 'student'
  AND s.user_uuid IS NULL;

INSERT INTO instructors (user_uuid, full_name, admin_verified, created_by)
SELECT
    u.uuid,
    COALESCE(
        NULLIF(
            TRIM(CONCAT_WS(' ',
                NULLIF(TRIM(u.first_name), ''),
                NULLIF(TRIM(u.middle_name), ''),
                NULLIF(TRIM(u.last_name), '')
            )),
            ''
        ),
        'Unknown'
    ) AS full_name,
    false,
    'system'
FROM users u
JOIN user_domain_mapping udm
    ON udm.user_uuid = u.uuid
JOIN user_domain ud
    ON ud.uuid = udm.domain_uuid
LEFT JOIN instructors i
    ON i.user_uuid = u.uuid
WHERE lower(ud.domain_name) = 'instructor'
  AND i.user_uuid IS NULL;

INSERT INTO course_creators (user_uuid, full_name, admin_verified, created_by)
SELECT
    u.uuid,
    COALESCE(
        NULLIF(
            TRIM(CONCAT_WS(' ',
                NULLIF(TRIM(u.first_name), ''),
                NULLIF(TRIM(u.middle_name), ''),
                NULLIF(TRIM(u.last_name), '')
            )),
            ''
        ),
        'Unknown'
    ) AS full_name,
    false,
    'system'
FROM users u
JOIN user_domain_mapping udm
    ON udm.user_uuid = u.uuid
JOIN user_domain ud
    ON ud.uuid = udm.domain_uuid
LEFT JOIN course_creators cc
    ON cc.user_uuid = u.uuid
WHERE lower(ud.domain_name) = 'course_creator'
  AND cc.user_uuid IS NULL;
