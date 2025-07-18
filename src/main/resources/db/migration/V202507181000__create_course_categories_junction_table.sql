-- V202507181500__create_course_categories_many_to_many.sql
-- Create junction table for many-to-many relationship between courses and categories

-- First, create the junction table
CREATE TABLE course_category_mappings
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid      UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    category_uuid    UUID                     NOT NULL REFERENCES course_categories (uuid) ON DELETE CASCADE,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255),

    -- Ensure unique course-category combinations
    CONSTRAINT unique_course_category UNIQUE (course_uuid, category_uuid)
);

-- Create indexes for performance
CREATE INDEX idx_course_category_mappings_course_uuid ON course_category_mappings (course_uuid);
CREATE INDEX idx_course_category_mappings_category_uuid ON course_category_mappings (category_uuid);
CREATE INDEX idx_course_category_mappings_uuid ON course_category_mappings (uuid);

-- Migrate existing data from courses.category_uuid to the junction table
-- Only migrate courses that have a category_uuid set
INSERT INTO course_category_mappings (course_uuid, category_uuid, created_by, created_date)
SELECT
    c.uuid as course_uuid,
    c.category_uuid,
    c.created_by,
    c.created_date
FROM courses c
WHERE c.category_uuid IS NOT NULL;

-- Remove the old category_uuid column since we're not maintaining backward compatibility
ALTER TABLE courses DROP COLUMN category_uuid;

-- Add a comment explaining the relationship
COMMENT ON TABLE course_category_mappings IS 'Junction table for many-to-many relationship between courses and categories';
COMMENT ON COLUMN course_category_mappings.course_uuid IS 'Reference to the course';
COMMENT ON COLUMN course_category_mappings.category_uuid IS 'Reference to the category';