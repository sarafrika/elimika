-- How a learner qualifies for a course: no prerequisite, the instructor's permission, or the
-- prior courses listed in course_prerequisites. Courses that already list prior courses start as COURSES.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS prerequisite_type VARCHAR(30) NOT NULL DEFAULT 'NONE';
ALTER TABLE courses DROP CONSTRAINT IF EXISTS chk_courses_prerequisite_type;
ALTER TABLE courses ADD CONSTRAINT chk_courses_prerequisite_type
    CHECK (prerequisite_type IN ('NONE', 'INSTRUCTOR_PERMISSION', 'COURSES'));

UPDATE courses c
SET prerequisite_type = 'COURSES'
WHERE EXISTS (SELECT 1 FROM course_prerequisites p WHERE p.course_uuid = c.uuid);
