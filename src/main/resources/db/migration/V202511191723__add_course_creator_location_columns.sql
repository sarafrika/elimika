-- Capture course creator geographic coordinates just like instructors
ALTER TABLE course_creators
    ADD COLUMN lat  DECIMAL(15, 12),
    ADD COLUMN long DECIMAL(15, 12);

COMMENT ON COLUMN course_creators.lat IS 'Latitude coordinate for the course creator primary location';
COMMENT ON COLUMN course_creators.long IS 'Longitude coordinate for the course creator primary location';
