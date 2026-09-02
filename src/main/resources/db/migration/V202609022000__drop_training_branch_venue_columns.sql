-- A training branch is a location/campus, not a venue. The venue-ish attributes that were
-- overloaded onto it (venue_type, capacity) belong to venue resources (organisation_resources of
-- type VENUE, which are scoped to a branch and carry seat_capacity). Drop the overloaded columns.
ALTER TABLE training_branches DROP COLUMN IF EXISTS venue_type;
ALTER TABLE training_branches DROP COLUMN IF EXISTS capacity;
