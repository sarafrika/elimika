-- Renames Medusa-era columns to internal commerce naming.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_catalog_item' AND column_name = 'medusa_product_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_catalog_item RENAME COLUMN medusa_product_id TO product_code';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_catalog_item' AND column_name = 'medusa_variant_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_catalog_item RENAME COLUMN medusa_variant_id TO variant_code';
    END IF;
END $$;

ALTER TABLE commerce_catalog_item DROP CONSTRAINT IF EXISTS uq_catalog_variant;
ALTER TABLE commerce_catalog_item ADD CONSTRAINT uq_catalog_variant_code UNIQUE (variant_code);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_purchase' AND column_name = 'medusa_order_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_purchase RENAME COLUMN medusa_order_id TO order_id';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_purchase' AND column_name = 'medusa_display_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_purchase RENAME COLUMN medusa_display_id TO order_number';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_purchase' AND column_name = 'medusa_created_at'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_purchase RENAME COLUMN medusa_created_at TO order_created_at';
    END IF;
END $$;

ALTER TABLE commerce_purchase DROP CONSTRAINT IF EXISTS commerce_purchase_medusa_order_id_key;
ALTER TABLE commerce_purchase ADD CONSTRAINT commerce_purchase_order_id_key UNIQUE (order_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'commerce_purchase_item' AND column_name = 'medusa_line_item_id'
    ) THEN
        EXECUTE 'ALTER TABLE commerce_purchase_item RENAME COLUMN medusa_line_item_id TO line_item_id';
    END IF;
END $$;

DROP INDEX IF EXISTS uk_commerce_purchase_item_line_item;
CREATE UNIQUE INDEX uk_commerce_purchase_item_line_item ON commerce_purchase_item (line_item_id);
