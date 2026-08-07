ALTER TABLE instructors
    ADD COLUMN IF NOT EXISTS location_name VARCHAR(255);

ALTER TABLE course_creators
    ADD COLUMN IF NOT EXISTS location_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS lat DECIMAL(15, 12),
    ADD COLUMN IF NOT EXISTS long DECIMAL(15, 12);

ALTER TABLE training_branches
    ADD COLUMN IF NOT EXISTS lat DECIMAL(15, 12),
    ADD COLUMN IF NOT EXISTS long DECIMAL(15, 12);

COMMENT ON COLUMN instructors.location_name
    IS 'Name of the place the instructor searched for, stored alongside lat/long so the location reads back as a place rather than a coordinate pair.';
COMMENT ON COLUMN course_creators.location_name
    IS 'Name of the place the course creator searched for, stored alongside lat/long.';
COMMENT ON COLUMN training_branches.lat
    IS 'Latitude of the branch address, resolved when the address was searched.';
COMMENT ON COLUMN training_branches.long
    IS 'Longitude of the branch address, resolved when the address was searched.';
