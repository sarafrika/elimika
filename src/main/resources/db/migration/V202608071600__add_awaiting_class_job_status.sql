ALTER TABLE class_marketplace_jobs
    DROP CONSTRAINT chk_class_marketplace_jobs_status;

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_status
        CHECK (status IN ('OPEN', 'AWAITING_CLASS', 'FILLED', 'CANCELLED', 'EXPIRED'));

COMMENT ON COLUMN class_marketplace_jobs.status
    IS 'OPEN accepts applications; AWAITING_CLASS has an instructor selected and resources still held, pending class creation; FILLED has its class; CANCELLED and EXPIRED have released their holds.';
