-- Create scheduled instances table
-- This table stores concrete class instances placed on the calendar by the timetabling module

CREATE TABLE scheduled_instances (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Links to other modules
    class_definition_uuid   UUID NOT NULL, -- Foreign key to class_definitions
    instructor_uuid         UUID NOT NULL,
    
    -- Scheduling Information
    start_time              TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time                TIMESTAMP WITH TIME ZONE NOT NULL,
    timezone                VARCHAR(50) NOT NULL DEFAULT 'UTC',
    
    -- Denormalized/Cached Information from ClassDefinition
    title                   VARCHAR(255) NOT NULL,
    location_type           VARCHAR(20) NOT NULL,
    max_participants        INTEGER NOT NULL,

    -- Live Status
    status                  VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED')),
    cancellation_reason     TEXT,
    
    -- Audit Fields
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255) NOT NULL,
    updated_by              VARCHAR(255)
);

-- Indexes for performance optimization
CREATE INDEX idx_scheduled_instances_instructor_time ON scheduled_instances (instructor_uuid, start_time, end_time);
CREATE INDEX idx_scheduled_instances_definition_uuid ON scheduled_instances (class_definition_uuid);
CREATE INDEX idx_scheduled_instances_status ON scheduled_instances (status);
CREATE INDEX idx_scheduled_instances_start_time ON scheduled_instances (start_time);
CREATE INDEX idx_scheduled_instances_time_range ON scheduled_instances (start_time, end_time);

-- Comments for documentation
COMMENT ON TABLE scheduled_instances IS 'Stores concrete scheduled class instances placed on the calendar';
COMMENT ON COLUMN scheduled_instances.class_definition_uuid IS 'Reference to the class definition from the classes module';
COMMENT ON COLUMN scheduled_instances.instructor_uuid IS 'Reference to the instructor conducting this session';
COMMENT ON COLUMN scheduled_instances.start_time IS 'Actual start time of the scheduled class session';
COMMENT ON COLUMN scheduled_instances.end_time IS 'Actual end time of the scheduled class session';
COMMENT ON COLUMN scheduled_instances.timezone IS 'Timezone for the scheduled session';
COMMENT ON COLUMN scheduled_instances.title IS 'Denormalized title from class definition for performance';
COMMENT ON COLUMN scheduled_instances.location_type IS 'Denormalized location type from class definition';
COMMENT ON COLUMN scheduled_instances.max_participants IS 'Denormalized max participants from class definition';
COMMENT ON COLUMN scheduled_instances.status IS 'Current status of the scheduled instance';
COMMENT ON COLUMN scheduled_instances.cancellation_reason IS 'Reason for cancellation if status is CANCELLED';