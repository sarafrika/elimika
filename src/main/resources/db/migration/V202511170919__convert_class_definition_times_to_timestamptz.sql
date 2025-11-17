-- Convert default start/end time fields to store full date-time (timestamp with time zone)
-- Existing time-only values are preserved with a neutral anchor date (1970-01-01 UTC)

ALTER TABLE class_definitions
    ALTER COLUMN default_start_time TYPE TIMESTAMP WITH TIME ZONE
        USING (DATE '1970-01-01' + default_start_time),
    ALTER COLUMN default_end_time TYPE TIMESTAMP WITH TIME ZONE
        USING (DATE '1970-01-01' + default_end_time);

COMMENT ON COLUMN class_definitions.default_start_time IS 'Default start date-time (time previously; now stores full date-time, UTC)';
COMMENT ON COLUMN class_definitions.default_end_time IS 'Default end date-time (time previously; now stores full date-time, UTC)';
