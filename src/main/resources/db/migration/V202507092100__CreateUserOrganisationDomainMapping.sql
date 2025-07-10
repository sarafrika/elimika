-- V202507092100__CreateUserOrganisationDomainMapping.sql
-- Migration: Create user-organization-domain mapping table
-- This creates a many-to-many relationship between users and organizations
-- while specifying the user's domain (role) within each organization

CREATE TABLE IF NOT EXISTS user_organisation_domain_mapping
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_uuid         UUID        NOT NULL,
    organisation_uuid UUID        NOT NULL,
    domain_uuid       UUID        NOT NULL,
    branch_uuid       UUID        NULL, -- Optional: specific branch assignment
    active            BOOLEAN     NOT NULL        DEFAULT true,
    start_date        DATE        NOT NULL        DEFAULT CURRENT_DATE,
    end_date          DATE        NULL, -- NULL means ongoing
    created_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50) NOT NULL,
    updated_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN     NOT NULL        DEFAULT FALSE,

    -- Foreign key constraints
    CONSTRAINT fk_user_org_domain_user
        FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_user_org_domain_organisation
        FOREIGN KEY (organisation_uuid) REFERENCES organisation (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_user_org_domain_domain
        FOREIGN KEY (domain_uuid) REFERENCES user_domain (uuid) ON DELETE RESTRICT,
    CONSTRAINT fk_user_org_domain_branch
        FOREIGN KEY (branch_uuid) REFERENCES training_branches (uuid) ON DELETE SET NULL,

    -- Business rules constraints
    CONSTRAINT chk_valid_date_range CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE UNIQUE INDEX uk_user_org_active_domain ON user_organisation_domain_mapping (user_uuid, organisation_uuid, active)
    WHERE active = 't' AND deleted = 'f';

-- Performance indexes
CREATE INDEX idx_user_org_domain_user_uuid ON user_organisation_domain_mapping (user_uuid);
CREATE INDEX idx_user_org_domain_organisation_uuid ON user_organisation_domain_mapping (organisation_uuid);
CREATE INDEX idx_user_org_domain_domain_uuid ON user_organisation_domain_mapping (domain_uuid);
CREATE INDEX idx_user_org_domain_branch_uuid ON user_organisation_domain_mapping (branch_uuid);
CREATE INDEX idx_user_org_domain_active ON user_organisation_domain_mapping (active);
CREATE INDEX idx_user_org_domain_composite ON user_organisation_domain_mapping (user_uuid, organisation_uuid, active);

-- Index for date range queries
CREATE INDEX idx_user_org_domain_date_range ON user_organisation_domain_mapping (start_date, end_date);

-- Comments for clarity
COMMENT ON TABLE user_organisation_domain_mapping IS 'Maps users to organizations with their specific domain/role';
COMMENT ON COLUMN user_organisation_domain_mapping.domain_uuid IS 'User role in this organization (student, instructor, admin, etc.)';
COMMENT ON COLUMN user_organisation_domain_mapping.branch_uuid IS 'Optional: specific branch assignment within organization';
COMMENT ON COLUMN user_organisation_domain_mapping.start_date IS 'When user started in this role at this organization';
COMMENT ON COLUMN user_organisation_domain_mapping.end_date IS 'When user ended this role (NULL = ongoing)';