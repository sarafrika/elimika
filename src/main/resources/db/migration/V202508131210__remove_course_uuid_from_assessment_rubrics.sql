-- Remove the direct course_uuid column from assessment_rubrics table
-- This supports the new many-to-many relationship via course_rubric_associations

-- Drop any existing foreign key constraints on course_uuid if they exist
-- Note: Using IF EXISTS to handle cases where constraints might not exist
DO $$ 
BEGIN 
    -- Drop foreign key constraint if it exists
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'fk_assessment_rubrics_course' 
               AND table_name = 'assessment_rubrics') THEN
        ALTER TABLE assessment_rubrics DROP CONSTRAINT assessment_rubrics_course_uuid_fkey;
    END IF;
END $$;

-- Drop the course_uuid column
ALTER TABLE assessment_rubrics DROP COLUMN IF EXISTS course_uuid;

-- Add comment explaining the change
COMMENT ON TABLE assessment_rubrics IS 'Assessment rubrics are now decoupled from courses. Use course_rubric_associations table for many-to-many relationships between courses and rubrics.';