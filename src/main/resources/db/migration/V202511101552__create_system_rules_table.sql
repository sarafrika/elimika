-- Central rule registry to orchestrate platform-wide decisions (fees, age gates, etc.)
CREATE TABLE system_rules
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    rule_category    VARCHAR(64)  NOT NULL,
    rule_key         VARCHAR(128) NOT NULL,
    rule_scope       VARCHAR(64)  NOT NULL,
    scope_reference  VARCHAR(128),
    priority         INTEGER      NOT NULL        DEFAULT 0,
    rule_status      VARCHAR(32)  NOT NULL        DEFAULT 'DRAFT',
    value_type       VARCHAR(32)  NOT NULL        DEFAULT 'JSON',
    value_payload    JSONB        NOT NULL,
    conditions       JSONB,
    effective_from   TIMESTAMPTZ  NOT NULL,
    effective_to     TIMESTAMPTZ,
    created_date     TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(100) NOT NULL,
    updated_date     TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(100),

    CONSTRAINT uq_system_rule_identity
        UNIQUE (rule_category, rule_key, rule_scope, scope_reference, effective_from)
);

CREATE INDEX idx_system_rules_category_status
    ON system_rules (rule_category, rule_status, rule_scope);

CREATE INDEX idx_system_rules_effective_range
    ON system_rules (effective_from, effective_to);

CREATE INDEX idx_system_rules_scope_reference
    ON system_rules (scope_reference);

INSERT INTO system_rules (
    rule_category,
    rule_key,
    rule_scope,
    scope_reference,
    priority,
    rule_status,
    value_type,
    value_payload,
    conditions,
    effective_from,
    effective_to,
    created_by,
    updated_by
)
VALUES (
    'AGE_GATE',
    'student.onboarding.age_gate',
    'GLOBAL',
    NULL,
    0,
    'ACTIVE',
    'JSON',
    '{"minAge":5,"maxAge":18}'::jsonb,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    'system',
    'system'
);

ALTER TABLE commerce_purchase
    ADD COLUMN platform_fee_amount NUMERIC(19, 4),
    ADD COLUMN platform_fee_currency VARCHAR(12),
    ADD COLUMN platform_fee_rule_uuid UUID;
