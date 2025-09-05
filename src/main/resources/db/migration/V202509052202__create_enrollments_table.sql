-- Create enrollments table  
-- This table links students to scheduled instances and tracks attendance

CREATE TABLE enrollments (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    scheduled_instance_uuid UUID NOT NULL, -- Foreign key to scheduled_instances
    student_uuid            UUID NOT NULL,
    
    -- Status & Attendance
    status                  VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED', 'ATTENDED', 'ABSENT', 'CANCELLED')),
    attendance_marked_at    TIMESTAMP WITH TIME ZONE,
    
    -- Audit Fields
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255) NOT NULL,
    updated_by              VARCHAR(255),

    CONSTRAINT unique_student_instance_enrollment UNIQUE (scheduled_instance_uuid, student_uuid)
);

-- Indexes for performance optimization
CREATE INDEX idx_enrollments_student_instance ON enrollments (student_uuid, scheduled_instance_uuid);
CREATE INDEX idx_enrollments_scheduled_instance ON enrollments (scheduled_instance_uuid);
CREATE INDEX idx_enrollments_student_uuid ON enrollments (student_uuid);
CREATE INDEX idx_enrollments_status ON enrollments (status);
CREATE INDEX idx_enrollments_attendance_date ON enrollments (attendance_marked_at) WHERE attendance_marked_at IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE enrollments IS 'Stores student enrollments in scheduled class instances and tracks attendance';
COMMENT ON COLUMN enrollments.scheduled_instance_uuid IS 'Reference to the scheduled class instance';
COMMENT ON COLUMN enrollments.student_uuid IS 'Reference to the enrolled student';
COMMENT ON COLUMN enrollments.status IS 'Current enrollment and attendance status';
COMMENT ON COLUMN enrollments.attendance_marked_at IS 'Timestamp when attendance was marked for this enrollment';
COMMENT ON CONSTRAINT unique_student_instance_enrollment ON enrollments IS 'Ensures a student can only have one enrollment per scheduled instance';