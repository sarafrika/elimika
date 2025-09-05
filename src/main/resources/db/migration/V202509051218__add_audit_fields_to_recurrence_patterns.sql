-- Add missing audit fields to recurrence_patterns table
-- The RecurrencePattern entity extends BaseEntity which requires these audit fields

ALTER TABLE recurrence_patterns 
ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'system',
ADD COLUMN updated_by VARCHAR(255);

-- Remove the default after adding the column to match entity requirements
ALTER TABLE recurrence_patterns 
ALTER COLUMN created_by DROP DEFAULT;

-- Add comments for documentation
COMMENT ON COLUMN recurrence_patterns.created_by IS 'User who created this recurrence pattern';
COMMENT ON COLUMN recurrence_patterns.updated_by IS 'User who last updated this recurrence pattern';