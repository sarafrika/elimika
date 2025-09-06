-- Add time fields to class_definitions table and remove duration_minutes
-- These fields enable recurring class scheduling with specific start and end times

-- Add the new time columns as required fields
ALTER TABLE class_definitions 
ADD COLUMN default_start_time TIME NOT NULL DEFAULT '09:00:00',
ADD COLUMN default_end_time TIME NOT NULL DEFAULT '10:30:00';

-- Remove the default values after adding them (they were just for existing records)
ALTER TABLE class_definitions ALTER COLUMN default_start_time DROP DEFAULT;
ALTER TABLE class_definitions ALTER COLUMN default_end_time DROP DEFAULT;

-- Drop the old duration_minutes column since duration is now computed from times
ALTER TABLE class_definitions DROP COLUMN duration_minutes;

-- Add check constraint to ensure end time is after start time
ALTER TABLE class_definitions ADD CONSTRAINT chk_class_time_validity 
    CHECK (default_start_time < default_end_time);

-- Create index for time-based queries
CREATE INDEX idx_class_definitions_times ON class_definitions (default_start_time, default_end_time);

-- Add comments for documentation
COMMENT ON COLUMN class_definitions.default_start_time IS 'Default start time for class sessions when recurring schedule is generated';
COMMENT ON COLUMN class_definitions.default_end_time IS 'Default end time for class sessions when recurring schedule is generated';