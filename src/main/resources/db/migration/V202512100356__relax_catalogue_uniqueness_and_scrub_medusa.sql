-- Allow multiple catalogue mappings per course/class and remove legacy Medusa column names.

ALTER TABLE commerce_catalogue_item DROP CONSTRAINT IF EXISTS uq_catalog_course;
ALTER TABLE commerce_catalogue_item DROP CONSTRAINT IF EXISTS uq_catalog_class;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_catalogue_item' AND column_name = 'medusa_product_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_catalogue_item RENAME COLUMN medusa_product_id TO product_code';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_catalogue_item' AND column_name = 'medusa_variant_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_catalogue_item RENAME COLUMN medusa_variant_id TO variant_code';
    END IF;
END $$;
