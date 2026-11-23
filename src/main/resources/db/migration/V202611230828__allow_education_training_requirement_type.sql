-- Instructor educational requirements (e.g. a teaching licence) were offered by the course creator but
-- refused by the API: allow 'education' alongside the existing resource types.
ALTER TABLE course_training_requirements DROP CONSTRAINT IF EXISTS course_training_requirements_requirement_type_check;
ALTER TABLE course_training_requirements ADD CONSTRAINT course_training_requirements_requirement_type_check
    CHECK (requirement_type IN ('material', 'equipment', 'facility', 'other', 'education'));
