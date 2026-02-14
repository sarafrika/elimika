ALTER TABLE courses
    ADD COLUMN admin_approved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_courses_admin_approved ON courses (admin_approved);

ALTER TABLE training_programs
    ADD COLUMN admin_approved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_training_programs_admin_approved ON training_programs (admin_approved);

UPDATE courses
SET admin_approved = TRUE
WHERE LOWER(status) = 'published'
  AND active = TRUE;

UPDATE training_programs
SET admin_approved = TRUE
WHERE LOWER(status) = 'published'
  AND is_active = TRUE;
