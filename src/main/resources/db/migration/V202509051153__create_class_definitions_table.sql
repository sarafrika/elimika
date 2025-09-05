-- Create class_definitions table for storing class templates/blueprints
-- This replaces the concept of class_sessions and focuses on class definitions rather than scheduled instances

CREATE TABLE class_definitions (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Basic Information
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    
    -- Ownership & Association
    default_instructor_uuid UUID NOT NULL,
    organisation_uuid UUID,
    course_uuid      UUID,
    
    -- Default Scheduling Information
    duration_minutes INTEGER NOT NULL,
    
    -- Default Format
    location_type    VARCHAR(20) NOT NULL DEFAULT 'ONLINE' CHECK (location_type IN ('ONLINE', 'IN_PERSON', 'HYBRID')),
    
    -- Default Capacity
    max_participants INTEGER NOT NULL DEFAULT 50,
    allow_waitlist   BOOLEAN NOT NULL DEFAULT true,
    
    -- Recurrence Pattern
    recurrence_pattern_uuid UUID,
    
    -- Status
    is_active        BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit Fields
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255) NOT NULL,
    updated_by       VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX idx_class_definitions_instructor ON class_definitions(default_instructor_uuid);
CREATE INDEX idx_class_definitions_organisation ON class_definitions(organisation_uuid);
CREATE INDEX idx_class_definitions_course ON class_definitions(course_uuid);
CREATE INDEX idx_class_definitions_active ON class_definitions(is_active);
CREATE INDEX idx_class_definitions_recurrence ON class_definitions(recurrence_pattern_uuid);

-- Add comments for documentation
COMMENT ON TABLE class_definitions IS 'Stores class definition templates that define what a class is, independent of scheduling';
COMMENT ON COLUMN class_definitions.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN class_definitions.title IS 'Display name of the class';
COMMENT ON COLUMN class_definitions.description IS 'Detailed description of the class content and objectives';
COMMENT ON COLUMN class_definitions.default_instructor_uuid IS 'UUID of the default instructor for this class';
COMMENT ON COLUMN class_definitions.organisation_uuid IS 'UUID of the organization this class belongs to';
COMMENT ON COLUMN class_definitions.course_uuid IS 'UUID of the course this class is part of (optional)';
COMMENT ON COLUMN class_definitions.duration_minutes IS 'Default duration of the class in minutes';
COMMENT ON COLUMN class_definitions.location_type IS 'Default delivery format (ONLINE, IN_PERSON, HYBRID)';
COMMENT ON COLUMN class_definitions.max_participants IS 'Maximum number of participants allowed';
COMMENT ON COLUMN class_definitions.allow_waitlist IS 'Whether to allow waitlisting when max capacity is reached';
COMMENT ON COLUMN class_definitions.recurrence_pattern_uuid IS 'UUID of the recurrence pattern for repeating classes';
COMMENT ON COLUMN class_definitions.is_active IS 'Whether this class definition is currently active';