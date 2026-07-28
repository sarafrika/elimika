-- Category a class falls under.
--
-- Course-backed classes inherit their categories from the course record, so nothing is stored for
-- them. Training programs carry no per-class category, so the organisation chooses one when it
-- creates the class; that choice rides the marketplace job and is copied onto the class definition
-- when an instructor is assigned. Nullable on both tables — existing rows keep their behaviour.

ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS category_uuid UUID;
ALTER TABLE class_definitions ADD COLUMN IF NOT EXISTS category_uuid UUID;

CREATE INDEX IF NOT EXISTS idx_class_definitions_category ON class_definitions (category_uuid);

COMMENT ON COLUMN class_marketplace_jobs.category_uuid IS
    'Category chosen for a program-backed class; null for course-backed classes, which inherit the course categories.';
COMMENT ON COLUMN class_definitions.category_uuid IS
    'Category chosen for a program-backed class; null for course-backed classes, which inherit the course categories.';
