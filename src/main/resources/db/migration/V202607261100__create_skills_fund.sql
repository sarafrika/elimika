-- Organisation Skills Fund for the org dashboard "Skills Fund" page.
-- Funding sources (grants, sponsors, budget) and fund transactions
-- (allocations, disbursements, adjustments). KPIs are derived from these.

CREATE TABLE skills_fund_sources
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    organisation_uuid UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    source_type       VARCHAR(100),
    amount            NUMERIC(19, 2)           NOT NULL        DEFAULT 0,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_skills_fund_sources_organisation ON skills_fund_sources (organisation_uuid);

COMMENT ON TABLE skills_fund_sources IS
    'Funding sources contributing to an organisation''s skills fund (grants, sponsors, budget).';

CREATE TABLE skills_fund_transactions
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    organisation_uuid UUID                     NOT NULL,
    description       VARCHAR(500),
    target_name       VARCHAR(255),
    amount            NUMERIC(19, 2)           NOT NULL        DEFAULT 0,
    transaction_type  VARCHAR(50)              NOT NULL        DEFAULT 'Allocation',
    status            VARCHAR(50)              NOT NULL        DEFAULT 'Pending',
    transaction_date  TIMESTAMP WITH TIME ZONE,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_skills_fund_transactions_organisation ON skills_fund_transactions (organisation_uuid);

COMMENT ON TABLE skills_fund_transactions IS
    'Skills fund movements — allocations, disbursements and adjustments — with a status lifecycle.';
