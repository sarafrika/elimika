-- Adds organization support metadata and external key for user domains

ALTER TABLE user_domain
    ADD COLUMN IF NOT EXISTS org_supported BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS domain_key UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE user_domain
    ADD CONSTRAINT uq_user_domain_domain_key UNIQUE (domain_key);
