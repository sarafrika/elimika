ALTER TABLE class_marketplace_jobs
    ADD COLUMN IF NOT EXISTS rate_basis VARCHAR(20) NOT NULL DEFAULT 'PER_HOUR';

ALTER TABLE class_definitions
    ADD COLUMN IF NOT EXISTS rate_basis VARCHAR(20) NOT NULL DEFAULT 'PER_HOUR';

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_rate_basis
        CHECK (rate_basis IN ('PER_HOUR', 'PER_SESSION', 'PER_DAY'));

ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_rate_basis
        CHECK (rate_basis IN ('PER_HOUR', 'PER_SESSION', 'PER_DAY'));

COMMENT ON COLUMN class_marketplace_jobs.rate_basis
    IS 'Unit both sale_price and instructor_pay are quoted in, fixed by the contract this job represents. Existing rows are PER_HOUR because that is what every price already meant.';
COMMENT ON COLUMN class_definitions.rate_basis
    IS 'Copied from the job that created the class so the basis the deal was struck on cannot drift.';
