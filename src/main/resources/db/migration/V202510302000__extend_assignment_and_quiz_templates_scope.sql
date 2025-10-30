-- Extend assignments and quizzes with scope metadata for class-level scheduling

ALTER TABLE assignments
    ADD COLUMN scope VARCHAR(32) NOT NULL DEFAULT 'COURSE_TEMPLATE',
    ADD COLUMN class_definition_uuid UUID,
    ADD COLUMN source_assignment_uuid UUID;

ALTER TABLE assignments
    ADD CONSTRAINT chk_assignments_scope
        CHECK (scope IN ('COURSE_TEMPLATE', 'CLASS_CLONE'));

ALTER TABLE assignments
    ADD CONSTRAINT fk_assignments_class_definition
        FOREIGN KEY (class_definition_uuid)
            REFERENCES class_definitions (uuid)
            ON DELETE CASCADE;

ALTER TABLE assignments
    ADD CONSTRAINT fk_assignments_source_assignment
        FOREIGN KEY (source_assignment_uuid)
            REFERENCES assignments (uuid)
            ON DELETE SET NULL;

CREATE INDEX idx_assignments_class_scope
    ON assignments (class_definition_uuid, scope);

ALTER TABLE quizzes
    ADD COLUMN scope VARCHAR(32) NOT NULL DEFAULT 'COURSE_TEMPLATE',
    ADD COLUMN class_definition_uuid UUID,
    ADD COLUMN source_quiz_uuid UUID;

ALTER TABLE quizzes
    ADD CONSTRAINT chk_quizzes_scope
        CHECK (scope IN ('COURSE_TEMPLATE', 'CLASS_CLONE'));

ALTER TABLE quizzes
    ADD CONSTRAINT fk_quizzes_class_definition
        FOREIGN KEY (class_definition_uuid)
            REFERENCES class_definitions (uuid)
            ON DELETE CASCADE;

ALTER TABLE quizzes
    ADD CONSTRAINT fk_quizzes_source_quiz
        FOREIGN KEY (source_quiz_uuid)
            REFERENCES quizzes (uuid)
            ON DELETE SET NULL;

CREATE INDEX idx_quizzes_class_scope
    ON quizzes (class_definition_uuid, scope);
