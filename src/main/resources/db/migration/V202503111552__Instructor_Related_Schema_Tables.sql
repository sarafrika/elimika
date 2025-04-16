-- Define the proficiency level type
CREATE TYPE proficiency_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT');

-- Create Instructors Table
ALTER TABLE instructor
    RENAME TO instructors;

ALTER TABLE instructors
    ADD COLUMN user_uuid UUID NOT NULL UNIQUE;

CREATE INDEX idx_instructor_user_uuid ON instructors (user_uuid);