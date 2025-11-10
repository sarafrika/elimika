ALTER TABLE students
    ADD COLUMN demographic_tag VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_students_demographic_tag
    ON students (demographic_tag);
