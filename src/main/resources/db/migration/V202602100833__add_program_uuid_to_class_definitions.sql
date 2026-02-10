-- Allow class definitions to be linked to training programs (in addition to courses or standalone).

ALTER TABLE class_definitions
    ADD COLUMN program_uuid UUID;

ALTER TABLE class_definitions
    ADD CONSTRAINT fk_class_definitions_program_uuid
        FOREIGN KEY (program_uuid) REFERENCES training_programs (uuid);

CREATE INDEX idx_class_definitions_program ON class_definitions (program_uuid);

ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_single_learning_context
        CHECK (course_uuid IS NULL OR program_uuid IS NULL);

COMMENT ON COLUMN class_definitions.program_uuid IS 'UUID of the training program this class is part of (optional)';
