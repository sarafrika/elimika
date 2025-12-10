-- Allow null class_definition_uuid and add BLOCKED status for instructor calendar blocks

ALTER TABLE scheduled_instances
    ALTER COLUMN class_definition_uuid DROP NOT NULL;

-- Relax status constraint to include BLOCKED
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'scheduled_instances_status_check'
            AND table_name = 'scheduled_instances'
    ) THEN
        ALTER TABLE scheduled_instances DROP CONSTRAINT scheduled_instances_status_check;
    END IF;
END$$;

ALTER TABLE scheduled_instances
    ADD CONSTRAINT scheduled_instances_status_check
    CHECK (status IN ('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED', 'BLOCKED'));
