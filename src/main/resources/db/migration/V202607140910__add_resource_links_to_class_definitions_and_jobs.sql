-- Link classes back to their venue resource and originating marketplace job,
-- and allow marketplace jobs to expire (releasing their resource holds).

ALTER TABLE class_definitions
    ADD COLUMN venue_resource_uuid UUID REFERENCES organisation_resources (uuid),
    ADD COLUMN marketplace_job_uuid UUID REFERENCES class_marketplace_jobs (uuid);

CREATE INDEX idx_class_definitions_venue_resource_uuid
    ON class_definitions (venue_resource_uuid)
    WHERE venue_resource_uuid IS NOT NULL;

ALTER TABLE class_marketplace_jobs
    DROP CONSTRAINT chk_class_marketplace_jobs_status;

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_status
        CHECK (status IN ('OPEN', 'FILLED', 'CANCELLED', 'EXPIRED'));

COMMENT ON COLUMN class_definitions.venue_resource_uuid IS 'Venue resource the class sessions are booked into (null for classes without a managed venue)';
COMMENT ON COLUMN class_definitions.marketplace_job_uuid IS 'Marketplace job this class was created from at instructor assignment (null for directly created classes)';
