-- Add resolution tracking fields to class scheduling conflicts

ALTER TABLE class_scheduling_conflicts
    ADD COLUMN is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN resolved_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_class_scheduling_conflicts_is_resolved
    ON class_scheduling_conflicts (is_resolved);

COMMENT ON COLUMN class_scheduling_conflicts.is_resolved IS 'Indicates whether the conflict has been resolved';
COMMENT ON COLUMN class_scheduling_conflicts.resolved_at IS 'Timestamp when the conflict was resolved';
