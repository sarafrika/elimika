-- 202601311234__rename_training_program_instructor_to_course_creator.sql
-- Align training program ownership with course creators

-- Step 1: Add course_creator_uuid column
ALTER TABLE training_programs
    ADD COLUMN course_creator_uuid UUID;

-- Step 2: Create course_creator records for instructors referenced by training programs
INSERT INTO course_creators (user_uuid, full_name, bio, professional_headline, admin_verified, created_date, updated_date, created_by, updated_by)
SELECT DISTINCT
    i.user_uuid,
    i.full_name,
    i.bio,
    i.professional_headline,
    true,
    i.created_date,
    i.updated_date,
    i.created_by,
    i.updated_by
FROM instructors i
WHERE i.uuid IN (SELECT DISTINCT instructor_uuid FROM training_programs WHERE instructor_uuid IS NOT NULL)
AND i.user_uuid NOT IN (SELECT user_uuid FROM course_creators)
ON CONFLICT (user_uuid) DO NOTHING;

-- Step 3: Update programs to reference course creators
UPDATE training_programs tp
SET course_creator_uuid = cc.uuid
FROM instructors i
JOIN course_creators cc ON cc.user_uuid = i.user_uuid
WHERE tp.instructor_uuid = i.uuid
AND tp.course_creator_uuid IS NULL;

-- Step 4: Drop legacy index and foreign key
DROP INDEX IF EXISTS idx_training_programs_author_uuid;

ALTER TABLE training_programs
    DROP CONSTRAINT IF EXISTS training_programs_instructor_uuid_fkey;

-- Step 5: Remove instructor_uuid column
ALTER TABLE training_programs
    DROP COLUMN IF EXISTS instructor_uuid;

-- Step 6: Enforce course creator ownership
ALTER TABLE training_programs
    ALTER COLUMN course_creator_uuid SET NOT NULL;

ALTER TABLE training_programs
    ADD CONSTRAINT fk_training_programs_course_creator
        FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE RESTRICT;

CREATE INDEX idx_training_programs_course_creator_uuid ON training_programs (course_creator_uuid);

COMMENT ON COLUMN training_programs.course_creator_uuid IS 'Reference to course creator who created and owns this training program';
