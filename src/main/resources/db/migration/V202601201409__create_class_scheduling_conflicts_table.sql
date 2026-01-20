-- Create class scheduling conflicts table
-- Stores conflicts detected when scheduling class definition sessions

CREATE TABLE class_scheduling_conflicts (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    class_definition_uuid   UUID NOT NULL,
    requested_start         TIMESTAMP WITH TIME ZONE NOT NULL,
    requested_end           TIMESTAMP WITH TIME ZONE NOT NULL,
    reasons                 TEXT[] NOT NULL DEFAULT '{}',

    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255) NOT NULL,
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_class_scheduling_conflicts_definition_uuid
    ON class_scheduling_conflicts (class_definition_uuid);

CREATE INDEX idx_class_scheduling_conflicts_requested_start
    ON class_scheduling_conflicts (requested_start);

COMMENT ON TABLE class_scheduling_conflicts IS 'Stores scheduling conflicts detected during class definition scheduling';
COMMENT ON COLUMN class_scheduling_conflicts.class_definition_uuid IS 'Reference to the class definition';
COMMENT ON COLUMN class_scheduling_conflicts.requested_start IS 'Requested start time that conflicted';
COMMENT ON COLUMN class_scheduling_conflicts.requested_end IS 'Requested end time that conflicted';
COMMENT ON COLUMN class_scheduling_conflicts.reasons IS 'Conflict reason list';
