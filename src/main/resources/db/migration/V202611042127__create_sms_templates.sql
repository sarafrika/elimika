-- Admin-edited wording for the SMS the platform sends. A missing row means the template's built-in
-- default is used; resetting a template deletes its row.
CREATE TABLE sms_templates
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    template_key    VARCHAR(64) NOT NULL UNIQUE,
    body            TEXT        NOT NULL,
    enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by_name VARCHAR(160),
    created_date    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_date    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    CONSTRAINT chk_sms_template_key CHECK (template_key IN (
        'PAYMENT_RECEIVED', 'SEAT_RELEASED', 'WITHDRAWAL_CODE', 'PAYOUT_NUMBER_CODE')),
    CONSTRAINT chk_sms_template_body CHECK (length(body) BETWEEN 1 AND 480)
);

COMMENT ON TABLE sms_templates IS 'Admin overrides of system SMS wording; absent row = built-in default';
