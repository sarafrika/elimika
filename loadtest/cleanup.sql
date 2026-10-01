-- Removes what the load tests leave in the LOCAL database. Never run against staging.
--
--   docker compose -f docker/compose.local.yaml exec -T postgres psql -U elimika -d elimika < loadtest/cleanup.sql
--
-- Markers: k6 sends User-Agent 'elimika-loadtest/k6'; mixed.js names every row it creates 'LT-<vu>-<iter>-<ms>'
-- and deletes it in the same iteration, so the LT- deletes only catch rows from interrupted iterations.
BEGIN;

-- Request audit rows written by RequestTrackingFilter for k6 traffic.
DELETE FROM request_audit_log WHERE user_agent LIKE 'elimika-loadtest%';

-- Draft courses from mixed.js (course_creator draft_cycle). Children first; most child tables also cascade.
CREATE TEMP TABLE lt_courses ON COMMIT DROP AS
    SELECT uuid FROM courses WHERE name LIKE 'LT-%';
DELETE FROM lesson_contents WHERE lesson_uuid IN (SELECT uuid FROM lessons WHERE course_uuid IN (SELECT uuid FROM lt_courses));
DELETE FROM lessons WHERE course_uuid IN (SELECT uuid FROM lt_courses);
DELETE FROM course_category_mappings WHERE course_uuid IN (SELECT uuid FROM lt_courses);
DELETE FROM courses WHERE uuid IN (SELECT uuid FROM lt_courses);

-- Student groups from mixed.js (org_admin student_groups); members cascade.
DELETE FROM student_groups WHERE name LIKE 'LT-%';

-- Not removable by marker (documented in loadtest/README.md):
--  * PUT /students/{uuid}/skill-goals and PUT /instructors/{uuid}/location-search write back the same values.
--  * POST .../submissions/{uuid}/grade (only when submissions exist) re-grades a real submission;
--    its instructor_comments are set to 'LT- load test grade'.
--  * ENROL_CYCLE=1 enrolments for qa-outsider are cancelled through the API, not deleted.

COMMIT;

SELECT
    (SELECT count(*) FROM request_audit_log WHERE user_agent LIKE 'elimika-loadtest%') AS audit_rows_left,
    (SELECT count(*) FROM courses WHERE name LIKE 'LT-%')                               AS lt_courses_left,
    (SELECT count(*) FROM student_groups WHERE name LIKE 'LT-%')                        AS lt_groups_left;
