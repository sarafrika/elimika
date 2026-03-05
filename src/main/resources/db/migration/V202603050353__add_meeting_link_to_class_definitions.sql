-- Add optional meeting link support to class definitions for virtual delivery access.

ALTER TABLE class_definitions
    ADD COLUMN meeting_link VARCHAR(1000);

COMMENT ON COLUMN class_definitions.meeting_link IS 'Optional virtual meeting URL for class delivery (e.g., Zoom, Google Meet, Teams)';
