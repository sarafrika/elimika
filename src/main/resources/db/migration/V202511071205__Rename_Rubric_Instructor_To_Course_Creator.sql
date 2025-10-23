-- Align assessment rubric ownership with course creators instead of instructors

-- Drop existing foreign key that ties rubrics to instructors
ALTER TABLE assessment_rubrics
    DROP CONSTRAINT IF EXISTS assessment_rubrics_instructor_uuid_fkey;

-- Rename the column to reflect course creator ownership
ALTER TABLE assessment_rubrics
    RENAME COLUMN instructor_uuid TO course_creator_uuid;

-- Recreate the foreign key pointing to course_creators
ALTER TABLE assessment_rubrics
    ADD CONSTRAINT assessment_rubrics_course_creator_uuid_fkey
        FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid);

-- Rename the supporting index to keep naming consistent
ALTER INDEX IF EXISTS idx_assessment_rubrics_author_uuid
    RENAME TO idx_assessment_rubrics_course_creator_uuid;

-- Document the relationship for clarity
COMMENT ON COLUMN assessment_rubrics.course_creator_uuid
    IS 'Reference to the course creator who defined this rubric';
