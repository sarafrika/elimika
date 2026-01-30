-- Backfill course enrollments from class and program enrollments

WITH class_enrollments_by_course AS (
    SELECT ce.student_uuid,
           cd.course_uuid,
           ce.status,
           COALESCE(ce.updated_date, ce.created_date) AS activity_at
    FROM class_enrollments ce
    JOIN scheduled_instances si ON ce.scheduled_instance_uuid = si.uuid
    JOIN class_definitions cd ON si.class_definition_uuid = cd.uuid
    WHERE cd.course_uuid IS NOT NULL
),
class_active AS (
    SELECT student_uuid,
           course_uuid,
           MAX(CASE WHEN status = 'ENROLLED' THEN 1 ELSE 0 END) AS has_active
    FROM class_enrollments_by_course
    GROUP BY student_uuid, course_uuid
),
class_latest AS (
    SELECT student_uuid,
           course_uuid,
           status,
           ROW_NUMBER() OVER (PARTITION BY student_uuid, course_uuid ORDER BY activity_at DESC) AS rn
    FROM class_enrollments_by_course
),
class_status AS (
    SELECT l.student_uuid,
           l.course_uuid,
           CASE
               WHEN a.has_active = 1 THEN 'active'
               ELSE CASE l.status
                   WHEN 'WAITLISTED' THEN 'suspended'
                   WHEN 'ATTENDED' THEN 'completed'
                   WHEN 'ABSENT' THEN 'dropped'
                   WHEN 'CANCELLED' THEN 'dropped'
                   WHEN 'ENROLLED' THEN 'active'
                   ELSE 'active'
               END
           END AS status
    FROM class_latest l
    JOIN class_active a ON a.student_uuid = l.student_uuid AND a.course_uuid = l.course_uuid
    WHERE l.rn = 1
),
program_enrollments_by_course AS (
    SELECT pe.student_uuid,
           pc.course_uuid,
           pe.status,
           COALESCE(pe.updated_date, pe.created_date) AS activity_at
    FROM program_enrollments pe
    JOIN program_courses pc ON pc.program_uuid = pe.program_uuid
    WHERE pc.course_uuid IS NOT NULL
),
program_latest AS (
    SELECT student_uuid,
           course_uuid,
           status,
           ROW_NUMBER() OVER (PARTITION BY student_uuid, course_uuid ORDER BY activity_at DESC) AS rn
    FROM program_enrollments_by_course
),
program_status AS (
    SELECT student_uuid,
           course_uuid,
           CASE LOWER(status)
               WHEN 'active' THEN 'active'
               WHEN 'completed' THEN 'completed'
               WHEN 'dropped' THEN 'dropped'
               WHEN 'suspended' THEN 'suspended'
               ELSE 'active'
           END AS status
    FROM program_latest
    WHERE rn = 1
),
resolved_status AS (
    SELECT c.student_uuid, c.course_uuid, c.status
    FROM class_status c
    UNION ALL
    SELECT p.student_uuid, p.course_uuid, p.status
    FROM program_status p
    LEFT JOIN class_status c
        ON c.student_uuid = p.student_uuid AND c.course_uuid = p.course_uuid
    WHERE c.student_uuid IS NULL
)
INSERT INTO course_enrollments (student_uuid, course_uuid, status, created_by, updated_by)
SELECT student_uuid, course_uuid, status, 'system-migration', 'system-migration'
FROM resolved_status
ON CONFLICT (student_uuid, course_uuid) DO UPDATE
SET status = EXCLUDED.status,
    updated_by = 'system-migration',
    updated_date = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours')
WHERE course_enrollments.status IS DISTINCT FROM EXCLUDED.status;
