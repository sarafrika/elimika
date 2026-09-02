-- Venues (and equipment) live inside a training branch, which already carries the geo-location.
-- A venue is a room/lab/theatre identified by its name, branch and seat capacity; it does not need
-- its own map destination. Drop the per-resource map coordinates (location_name is kept as an
-- optional within-branch label such as 'Main Hall, 2nd floor').
ALTER TABLE organisation_resources DROP COLUMN IF EXISTS location_latitude;
ALTER TABLE organisation_resources DROP COLUMN IF EXISTS location_longitude;
