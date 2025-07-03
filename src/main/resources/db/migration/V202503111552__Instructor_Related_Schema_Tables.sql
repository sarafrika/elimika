-- Define the proficiency level type
CREATE TYPE proficiency_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT');

-- Create Instructors Table
ALTER TABLE instructor
    RENAME TO instructors;

ALTER TABLE instructors
    ADD COLUMN user_uuid UUID NOT NULL UNIQUE,
    ADD COLUMN admin_verified bool NOT NULL DEFAULT false;

-- Add missing foreign key constraint
ALTER TABLE instructors
    ADD CONSTRAINT fk_instructors_user_uuid
        FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE ON UPDATE CASCADE;

CREATE INDEX idx_instructor_user_uuid ON instructors (user_uuid);