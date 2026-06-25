ALTER TABLE class_marketplace_jobs
    ADD COLUMN program_uuid UUID;

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT fk_class_marketplace_jobs_program_uuid
        FOREIGN KEY (program_uuid) REFERENCES training_programs (uuid);

ALTER TABLE class_marketplace_jobs
    ALTER COLUMN course_uuid DROP NOT NULL;

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_single_learning_context
        CHECK (
            (course_uuid IS NOT NULL AND program_uuid IS NULL)
            OR (course_uuid IS NULL AND program_uuid IS NOT NULL)
        );

CREATE INDEX idx_class_marketplace_jobs_program_uuid
    ON class_marketplace_jobs (program_uuid);

COMMENT ON COLUMN class_marketplace_jobs.program_uuid IS 'UUID of the training program backing this marketplace class job when it is program-scoped';
