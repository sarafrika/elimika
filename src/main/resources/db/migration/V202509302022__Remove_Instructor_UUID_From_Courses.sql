-- Remove the mutual exclusivity check constraint first
ALTER TABLE courses
DROP CONSTRAINT IF EXISTS chk_course_owner;

-- Drop the index on instructor_uuid
DROP INDEX IF EXISTS idx_courses_instructor_uuid;

-- Remove the foreign key constraint if it exists
ALTER TABLE courses
DROP CONSTRAINT IF EXISTS fk_courses_instructor;

-- Remove the instructor_uuid column
ALTER TABLE courses
DROP COLUMN IF EXISTS instructor_uuid;

-- Update the check constraint to only require course_creator_uuid
ALTER TABLE courses
ADD CONSTRAINT chk_course_creator_required CHECK (course_creator_uuid IS NOT NULL);

-- Add comment for clarity
COMMENT ON COLUMN courses.course_creator_uuid IS 'Reference to course creator who created and owns this course (required)';