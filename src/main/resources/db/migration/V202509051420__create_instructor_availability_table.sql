-- Create instructor availability table
-- This table stores availability patterns for instructors including daily, weekly, monthly, and custom patterns

CREATE TABLE instructor_availability (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    instructor_uuid   UUID NOT NULL,
    
    -- Availability Type
    availability_type VARCHAR(20) NOT NULL DEFAULT 'weekly' CHECK (availability_type IN ('daily', 'weekly', 'monthly', 'custom')),
    
    -- Time Pattern Information
    day_of_week       INTEGER, -- 1 (Monday) to 7 (Sunday), for weekly availability
    day_of_month      INTEGER, -- 1-31, for monthly availability
    specific_date     DATE,    -- For custom patterns or one-time availability
    start_time        TIME NOT NULL,
    end_time          TIME NOT NULL,
    
    -- Custom Pattern Support
    custom_pattern    VARCHAR(255), -- For cron-like expressions or complex patterns
    
    -- Recurrence Configuration
    recurrence_interval INTEGER DEFAULT 1, -- Interval for pattern recurrence (e.g., every 2 weeks)
    
    -- Effective Period
    effective_start_date DATE, -- When this availability pattern starts being effective
    effective_end_date   DATE, -- When this availability pattern stops being effective
    
    -- Status
    is_available      BOOLEAN NOT NULL DEFAULT true, -- true for available, false for blocked out
    
    -- Audit Fields
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    updated_by        VARCHAR(255),

    -- Constraints
    CONSTRAINT check_time_validity CHECK (start_time < end_time),
    CONSTRAINT check_day_of_week_range CHECK (day_of_week IS NULL OR (day_of_week >= 1 AND day_of_week <= 7)),
    CONSTRAINT check_day_of_month_range CHECK (day_of_month IS NULL OR (day_of_month >= 1 AND day_of_month <= 31)),
    CONSTRAINT check_effective_date_validity CHECK (effective_start_date IS NULL OR effective_end_date IS NULL OR effective_start_date <= effective_end_date),
    CONSTRAINT check_recurrence_interval_positive CHECK (recurrence_interval IS NULL OR recurrence_interval > 0),
    
    -- Conditional constraints based on availability type
    CONSTRAINT check_weekly_day_of_week CHECK (
        availability_type != 'weekly' OR day_of_week IS NOT NULL
    ),
    CONSTRAINT check_monthly_day_of_month CHECK (
        availability_type != 'monthly' OR day_of_month IS NOT NULL  
    ),
    CONSTRAINT check_custom_pattern CHECK (
        availability_type != 'custom' OR (custom_pattern IS NOT NULL AND LENGTH(TRIM(custom_pattern)) > 0)
    )
);

-- Indexes for performance optimization
CREATE INDEX idx_instructor_availability_instructor_uuid ON instructor_availability (instructor_uuid);
CREATE INDEX idx_instructor_availability_type ON instructor_availability (availability_type);
CREATE INDEX idx_instructor_availability_instructor_type ON instructor_availability (instructor_uuid, availability_type);
CREATE INDEX idx_instructor_availability_day_of_week ON instructor_availability (day_of_week) WHERE availability_type = 'weekly';
CREATE INDEX idx_instructor_availability_day_of_month ON instructor_availability (day_of_month) WHERE availability_type = 'monthly';
CREATE INDEX idx_instructor_availability_specific_date ON instructor_availability (specific_date) WHERE specific_date IS NOT NULL;
CREATE INDEX idx_instructor_availability_effective_dates ON instructor_availability (effective_start_date, effective_end_date);
CREATE INDEX idx_instructor_availability_time_range ON instructor_availability (start_time, end_time);
CREATE INDEX idx_instructor_availability_is_available ON instructor_availability (is_available);

-- Comments for documentation
COMMENT ON TABLE instructor_availability IS 'Stores instructor availability patterns supporting daily, weekly, monthly, and custom scheduling patterns';
COMMENT ON COLUMN instructor_availability.availability_type IS 'Type of availability pattern: daily, weekly, monthly, or custom';
COMMENT ON COLUMN instructor_availability.day_of_week IS 'Day of week for weekly patterns (1=Monday, 7=Sunday)';
COMMENT ON COLUMN instructor_availability.day_of_month IS 'Day of month for monthly patterns (1-31)';
COMMENT ON COLUMN instructor_availability.specific_date IS 'Specific date for custom patterns or one-time availability/blocking';
COMMENT ON COLUMN instructor_availability.custom_pattern IS 'Custom pattern expression (e.g., cron-like) for complex scheduling rules';
COMMENT ON COLUMN instructor_availability.recurrence_interval IS 'Recurrence interval (e.g., every 2 weeks for weekly type)';
COMMENT ON COLUMN instructor_availability.effective_start_date IS 'Date when this availability pattern becomes effective';
COMMENT ON COLUMN instructor_availability.effective_end_date IS 'Date when this availability pattern expires';
COMMENT ON COLUMN instructor_availability.is_available IS 'true for availability slots, false for blocked time slots';