-- Drop Course Bundle Tables Migration
-- Removes all course bundling functionality and related tables

-- Drop indexes first
DROP INDEX IF EXISTS idx_course_bundle_courses_bundle_required;
DROP INDEX IF EXISTS idx_course_bundle_courses_bundle_sequence;
DROP INDEX IF EXISTS idx_course_bundle_courses_course_uuid;
DROP INDEX IF EXISTS idx_course_bundle_courses_bundle_uuid;

DROP INDEX IF EXISTS idx_course_bundles_created_date;
DROP INDEX IF EXISTS idx_course_bundles_name;
DROP INDEX IF EXISTS idx_course_bundles_status_active;
DROP INDEX IF EXISTS idx_course_bundles_status;
DROP INDEX IF EXISTS idx_course_bundles_instructor_uuid;

-- Drop foreign key constraints (if they exist)
-- ALTER TABLE course_bundle_courses DROP CONSTRAINT IF EXISTS fk_course_bundle_courses_course;
-- ALTER TABLE course_bundle_courses DROP CONSTRAINT IF EXISTS fk_course_bundle_courses_bundle;

-- Drop tables (junction table first due to dependencies)
DROP TABLE IF EXISTS course_bundle_courses;
DROP TABLE IF EXISTS course_bundles;