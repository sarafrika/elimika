-- Adds an optional per-session training fee to organisation-posted class marketplace jobs.
-- The fee is carried onto the created class definition when an instructor is assigned.
ALTER TABLE class_marketplace_jobs
    ADD COLUMN training_fee NUMERIC(12, 2);

COMMENT ON COLUMN class_marketplace_jobs.training_fee
    IS 'Optional fee charged per session for the advertised class; copied to the class definition on instructor assignment.';
