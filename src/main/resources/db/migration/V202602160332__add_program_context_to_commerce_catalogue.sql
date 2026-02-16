-- Add training program context to commerce catalogue and internal commerce products.

ALTER TABLE commerce_catalogue_item
    ADD COLUMN program_uuid UUID;

ALTER TABLE commerce_catalogue_item
    ADD CONSTRAINT fk_catalog_program
        FOREIGN KEY (program_uuid) REFERENCES training_programs (uuid) ON DELETE CASCADE;

CREATE INDEX idx_catalog_program_uuid ON commerce_catalogue_item (program_uuid);

ALTER TABLE commerce_catalogue_item
    DROP CONSTRAINT IF EXISTS chk_course_or_class;

ALTER TABLE commerce_catalogue_item
    ADD CONSTRAINT chk_catalog_learning_context
        CHECK (
            (course_uuid IS NOT NULL)
            OR (class_definition_uuid IS NOT NULL)
            OR (program_uuid IS NOT NULL)
        );

UPDATE commerce_catalogue_item item
SET program_uuid = class_def.program_uuid
FROM class_definitions class_def
WHERE item.class_definition_uuid = class_def.uuid
  AND item.program_uuid IS NULL
  AND class_def.program_uuid IS NOT NULL;

ALTER TABLE commerce_product
    ADD COLUMN program_uuid UUID;

ALTER TABLE commerce_product
    ADD CONSTRAINT fk_product_program
        FOREIGN KEY (program_uuid) REFERENCES training_programs (uuid) ON DELETE CASCADE;

CREATE INDEX idx_product_program_uuid ON commerce_product (program_uuid);

ALTER TABLE commerce_product
    DROP CONSTRAINT IF EXISTS chk_product_course_or_class;

ALTER TABLE commerce_product
    ADD CONSTRAINT chk_product_learning_context
        CHECK (
            (course_uuid IS NOT NULL)
            OR (class_definition_uuid IS NOT NULL)
            OR (program_uuid IS NOT NULL)
        );

UPDATE commerce_product product
SET program_uuid = class_def.program_uuid
FROM class_definitions class_def
WHERE product.class_definition_uuid = class_def.uuid
  AND product.program_uuid IS NULL
  AND class_def.program_uuid IS NOT NULL;
