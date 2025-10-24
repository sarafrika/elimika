-- Rename enrollments table to clarify its purpose for class enrollment tracking

ALTER TABLE enrollments
    RENAME TO class_enrollments;

-- Update supporting index names to match the new table name
ALTER INDEX idx_enrollments_student_instance
    RENAME TO idx_class_enrollments_student_instance;

ALTER INDEX idx_enrollments_scheduled_instance
    RENAME TO idx_class_enrollments_scheduled_instance;

ALTER INDEX idx_enrollments_student_uuid
    RENAME TO idx_class_enrollments_student_uuid;

ALTER INDEX idx_enrollments_status
    RENAME TO idx_class_enrollments_status;

ALTER INDEX idx_enrollments_attendance_date
    RENAME TO idx_class_enrollments_attendance_date;

-- Refresh constraint naming for consistency
ALTER TABLE class_enrollments
    RENAME CONSTRAINT unique_student_instance_enrollment TO uq_class_enrollments_student_instance;

-- Re-apply documentation comments using the updated naming
COMMENT ON TABLE class_enrollments IS 'Stores student enrollments in scheduled class instances and tracks attendance';
COMMENT ON COLUMN class_enrollments.scheduled_instance_uuid IS 'Reference to the scheduled class instance';
COMMENT ON COLUMN class_enrollments.student_uuid IS 'Reference to the enrolled student';
COMMENT ON COLUMN class_enrollments.status IS 'Current enrollment and attendance status';
COMMENT ON COLUMN class_enrollments.attendance_marked_at IS 'Timestamp when attendance was marked for this enrollment';
COMMENT ON CONSTRAINT uq_class_enrollments_student_instance ON class_enrollments IS 'Ensures a student can only have one enrollment per scheduled instance';
