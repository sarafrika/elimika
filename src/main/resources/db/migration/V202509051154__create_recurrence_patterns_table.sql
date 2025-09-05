-- Create recurrence_patterns table for defining class recurrence rules
-- This table supports complex recurrence patterns for class scheduling

CREATE TABLE recurrence_patterns (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Recurrence Configuration
    recurrence_type  VARCHAR(20) NOT NULL CHECK (recurrence_type IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    interval_value   INTEGER DEFAULT 1,
    
    -- Specific Day Configuration
    days_of_week     VARCHAR(100), -- e.g., 'MONDAY,WEDNESDAY,FRIDAY'
    day_of_month     INTEGER,      -- For monthly recurrence (1-31)
    
    -- End Conditions
    end_date         DATE,         -- Optional end date for recurrence
    occurrence_count INTEGER,      -- Optional max number of occurrences
    
    -- Audit Fields
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_recurrence_patterns_type ON recurrence_patterns(recurrence_type);
CREATE INDEX idx_recurrence_patterns_end_date ON recurrence_patterns(end_date);

-- Add check constraints for data integrity
ALTER TABLE recurrence_patterns ADD CONSTRAINT chk_interval_positive 
    CHECK (interval_value > 0);

ALTER TABLE recurrence_patterns ADD CONSTRAINT chk_day_of_month_valid 
    CHECK (day_of_month IS NULL OR (day_of_month >= 1 AND day_of_month <= 31));

ALTER TABLE recurrence_patterns ADD CONSTRAINT chk_occurrence_count_positive 
    CHECK (occurrence_count IS NULL OR occurrence_count > 0);

-- Add comments for documentation
COMMENT ON TABLE recurrence_patterns IS 'Defines recurrence patterns for repeating classes';
COMMENT ON COLUMN recurrence_patterns.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN recurrence_patterns.recurrence_type IS 'Type of recurrence: DAILY, WEEKLY, or MONTHLY';
COMMENT ON COLUMN recurrence_patterns.interval_value IS 'Interval between recurrences (e.g., every 2 weeks)';
COMMENT ON COLUMN recurrence_patterns.days_of_week IS 'Comma-separated list of days for weekly recurrence';
COMMENT ON COLUMN recurrence_patterns.day_of_month IS 'Specific day of month for monthly recurrence';
COMMENT ON COLUMN recurrence_patterns.end_date IS 'Optional end date for the recurrence pattern';
COMMENT ON COLUMN recurrence_patterns.occurrence_count IS 'Optional maximum number of occurrences';