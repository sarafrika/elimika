ALTER TABLE scheduled_instances
    ADD COLUMN started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN concluded_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN scheduled_instances.started_at IS 'Actual UTC timestamp when the instructor explicitly started the scheduled class session';
COMMENT ON COLUMN scheduled_instances.concluded_at IS 'Actual UTC timestamp when the instructor explicitly concluded the scheduled class session';
