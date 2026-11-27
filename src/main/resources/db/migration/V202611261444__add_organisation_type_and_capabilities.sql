-- Organisation onboarding records what kind of body the organisation is and what it will do on the platform.
-- Both are optional so existing organisations stay valid. Allowed values live in OrganisationType and
-- OrganisationCapability, not in CHECK constraints, so they can change without a migration.
ALTER TABLE organisation
    ADD COLUMN IF NOT EXISTS organisation_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS capabilities      TEXT[] NOT NULL DEFAULT '{}';
