-- Remove lesson duration estimates to allow class scheduling to define timing
ALTER TABLE lessons
    DROP COLUMN duration_hours,
    DROP COLUMN duration_minutes;
