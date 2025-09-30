-- Add course_creator_uuid column to courses table
ALTER TABLE courses
ADD COLUMN course_creator_uuid UUID;

-- Add foreign key constraint
ALTER TABLE courses
ADD CONSTRAINT fk_courses_course_creator
    FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE RESTRICT;

-- Create index for performance
CREATE INDEX idx_courses_course_creator_uuid ON courses (course_creator_uuid);

-- Make instructor_uuid nullable (courses can now be created by either instructors or course creators)
ALTER TABLE courses
ALTER COLUMN instructor_uuid DROP NOT NULL;

-- Add check constraint to ensure either instructor_uuid or course_creator_uuid is set
ALTER TABLE courses
ADD CONSTRAINT chk_course_owner CHECK (
    (instructor_uuid IS NOT NULL AND course_creator_uuid IS NULL) OR
    (instructor_uuid IS NULL AND course_creator_uuid IS NOT NULL)
);

-- Comments for clarity
COMMENT ON COLUMN courses.course_creator_uuid IS 'Reference to course creator who created this course (mutually exclusive with instructor_uuid)';