-- Venue attributes for the organisation dashboard (Lovable "Venues" cards):
-- optional capacity and a free-form venue/room type. Both nullable — populated
-- over time; existing rows keep NULL.
ALTER TABLE training_branches ADD COLUMN IF NOT EXISTS capacity INTEGER;
ALTER TABLE training_branches ADD COLUMN IF NOT EXISTS venue_type VARCHAR(50);
