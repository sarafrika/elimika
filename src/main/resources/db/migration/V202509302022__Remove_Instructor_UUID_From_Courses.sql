-- Step 1: Create course_creator records for all instructors who have courses
INSERT INTO course_creators (user_uuid, full_name, bio, professional_headline, admin_verified, created_date, updated_date, created_by, updated_by)
SELECT DISTINCT
    i.user_uuid,
    i.full_name,
    i.bio,
    i.professional_headline,
    true, -- admin_verified
    i.created_date,
    i.updated_date,
    i.created_by,
    i.updated_by
FROM instructors i
WHERE i.uuid IN (SELECT DISTINCT instructor_uuid FROM courses WHERE instructor_uuid IS NOT NULL)
AND i.user_uuid NOT IN (SELECT user_uuid FROM course_creators)
ON CONFLICT (user_uuid) DO NOTHING;

-- Step 2: Update courses to reference the new course_creator_uuid
UPDATE courses c
SET course_creator_uuid = cc.uuid
FROM instructors i
JOIN course_creators cc ON cc.user_uuid = i.user_uuid
WHERE c.instructor_uuid = i.uuid
AND c.course_creator_uuid IS NULL;

-- Step 3: Remove the mutual exclusivity check constraint
ALTER TABLE courses
DROP CONSTRAINT IF EXISTS chk_course_owner;

-- Step 4: Drop the index on instructor_uuid
DROP INDEX IF EXISTS idx_courses_instructor_uuid;

-- Step 5: Remove the foreign key constraint if it exists
ALTER TABLE courses
DROP CONSTRAINT IF EXISTS fk_courses_instructor;

-- Step 6: Remove the instructor_uuid column
ALTER TABLE courses
DROP COLUMN IF EXISTS instructor_uuid;

-- Step 7: Update the check constraint to only require course_creator_uuid
ALTER TABLE courses
ADD CONSTRAINT chk_course_creator_required CHECK (course_creator_uuid IS NOT NULL);

-- Add comment for clarity
COMMENT ON COLUMN courses.course_creator_uuid IS 'Reference to course creator who created and owns this course (required)';