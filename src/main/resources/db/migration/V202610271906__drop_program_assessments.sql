-- Programme components are replaced by weighted programme courses.
DROP INDEX IF EXISTS idx_course_assessments_program_assessment;
ALTER TABLE course_assessments DROP COLUMN IF EXISTS program_assessment_uuid;
DROP TABLE IF EXISTS program_assessments;
