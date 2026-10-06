-- Course and program codes (unique among live rows; a course draft shares its live course's code),
-- pass marks as a final-grade percentage, and program thumbnail, banner and intro video.
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS course_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pass_mark   NUMERIC(5, 2);

ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS chk_courses_pass_mark;
ALTER TABLE courses
    ADD CONSTRAINT chk_courses_pass_mark CHECK (pass_mark IS NULL OR (pass_mark >= 0 AND pass_mark <= 100));

CREATE UNIQUE INDEX IF NOT EXISTS uq_courses_course_code_live
    ON courses (UPPER(course_code))
    WHERE course_code IS NOT NULL AND parent_course_uuid IS NULL;

ALTER TABLE training_programs
    ADD COLUMN IF NOT EXISTS program_code    VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pass_mark       NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS thumbnail_url   VARCHAR(500),
    ADD COLUMN IF NOT EXISTS banner_url      VARCHAR(500),
    ADD COLUMN IF NOT EXISTS intro_video_url VARCHAR(500);

ALTER TABLE training_programs
    DROP CONSTRAINT IF EXISTS chk_training_programs_pass_mark;
ALTER TABLE training_programs
    ADD CONSTRAINT chk_training_programs_pass_mark CHECK (pass_mark IS NULL OR (pass_mark >= 0 AND pass_mark <= 100));

CREATE UNIQUE INDEX IF NOT EXISTS uq_training_programs_program_code
    ON training_programs (UPPER(program_code))
    WHERE program_code IS NOT NULL;
