-- Add location fields to class_definitions and scheduled_instances for Mapbox support

ALTER TABLE class_definitions
    ADD COLUMN location_name VARCHAR(255),
    ADD COLUMN location_latitude NUMERIC(10,8),
    ADD COLUMN location_longitude NUMERIC(11,8);

COMMENT ON COLUMN class_definitions.location_name IS 'Human-readable name for the primary class location (e.g., campus or venue name)';
COMMENT ON COLUMN class_definitions.location_latitude IS 'Latitude coordinate for the primary class location, used with Mapbox';
COMMENT ON COLUMN class_definitions.location_longitude IS 'Longitude coordinate for the primary class location, used with Mapbox';

ALTER TABLE scheduled_instances
    ADD COLUMN location_name VARCHAR(255),
    ADD COLUMN location_latitude NUMERIC(10,8),
    ADD COLUMN location_longitude NUMERIC(11,8);

COMMENT ON COLUMN scheduled_instances.location_name IS 'Denormalized location name from class definition or per-instance override';
COMMENT ON COLUMN scheduled_instances.location_latitude IS 'Latitude coordinate for this scheduled instance location';
COMMENT ON COLUMN scheduled_instances.location_longitude IS 'Longitude coordinate for this scheduled instance location';
