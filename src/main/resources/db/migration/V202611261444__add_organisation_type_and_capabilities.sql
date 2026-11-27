-- Organisation onboarding records what kind of body the organisation is and what it will do on the platform.
-- Both are optional so existing organisations stay valid.
ALTER TABLE organisation
    ADD COLUMN IF NOT EXISTS organisation_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS capabilities      TEXT[] NOT NULL DEFAULT '{}';

ALTER TABLE organisation
    DROP CONSTRAINT IF EXISTS chk_organisation_type;

ALTER TABLE organisation
    ADD CONSTRAINT chk_organisation_type
        CHECK (organisation_type IS NULL OR organisation_type IN
                                             ('SCHOOL_OR_TRAINING_INSTITUTION', 'COLLEGE_OR_UNIVERSITY',
                                              'COMPANY_OR_EMPLOYER', 'FOUNDATION_OR_NGO', 'GOVERNMENT_AGENCY'));

ALTER TABLE organisation
    DROP CONSTRAINT IF EXISTS chk_organisation_capabilities;

ALTER TABLE organisation
    ADD CONSTRAINT chk_organisation_capabilities
        CHECK (capabilities <@ ARRAY ['TRAIN', 'PUBLISH', 'FUND', 'HIRE']::TEXT[]);
