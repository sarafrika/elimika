-- Drop unused recurrence pattern artifacts now replaced by inline session templates

-- Remove class_definitions.recurrence_pattern_uuid and its index if present
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'class_definitions' AND column_name = 'recurrence_pattern_uuid'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM pg_indexes WHERE tablename = 'class_definitions' AND indexname = 'idx_class_definitions_recurrence'
        ) THEN
            EXECUTE 'DROP INDEX IF EXISTS idx_class_definitions_recurrence';
        END IF;
        EXECUTE 'ALTER TABLE class_definitions DROP COLUMN IF EXISTS recurrence_pattern_uuid';
    END IF;
END$$;

-- Drop recurrence_patterns table if it exists
DROP TABLE IF EXISTS recurrence_patterns CASCADE;
