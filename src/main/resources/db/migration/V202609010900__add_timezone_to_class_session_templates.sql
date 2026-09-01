ALTER TABLE class_session_templates
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';

ALTER TABLE class_marketplace_job_session_templates
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';

COMMENT ON COLUMN class_session_templates.timezone IS 'IANA timezone used when displaying scheduled instances generated from this template';
COMMENT ON COLUMN class_marketplace_job_session_templates.timezone IS 'IANA timezone copied into generated class session templates when a marketplace job is assigned';
