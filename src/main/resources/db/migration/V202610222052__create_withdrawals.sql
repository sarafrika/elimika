-- Withdrawals from a wallet to a verified M-Pesa number, and the rules admins set for them.
-- Money leaves the balance when the SMS code is confirmed (a WITHDRAWAL row), and comes back as a
-- WITHDRAWAL_REVERSAL row if M-Pesa fails the payout or an admin rejects it.

CREATE TABLE wallet_payout_settings
(
    id                    SMALLINT PRIMARY KEY DEFAULT 1,
    min_amount            NUMERIC(18, 2) NOT NULL,
    max_amount            NUMERIC(18, 2) NOT NULL,
    daily_limit           NUMERIC(18, 2) NOT NULL,
    review_limit          NUMERIC(18, 2) NOT NULL,
    second_approval_limit NUMERIC(18, 2) NOT NULL,
    fee_payer             VARCHAR(20)    NOT NULL,
    earner_fee            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    updated_date          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    CONSTRAINT chk_wallet_payout_settings_single CHECK (id = 1),
    CONSTRAINT chk_wallet_payout_settings_fee_payer CHECK (fee_payer IN ('PLATFORM', 'EARNER')),
    CONSTRAINT chk_wallet_payout_settings_amounts CHECK (
        min_amount > 0 AND max_amount >= min_amount AND daily_limit >= max_amount
        AND review_limit > 0 AND second_approval_limit > 0 AND earner_fee >= 0)
);

INSERT INTO wallet_payout_settings (id, min_amount, max_amount, daily_limit, review_limit, second_approval_limit, fee_payer)
VALUES (1, 100, 150000, 300000, 50000, 100000, 'PLATFORM');

CREATE SEQUENCE withdrawal_reference_seq START 1;

CREATE TABLE withdrawals
(
    id                         BIGSERIAL PRIMARY KEY,
    uuid                       UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    reference                  VARCHAR(24)    NOT NULL UNIQUE,
    owner_type                 VARCHAR(20)    NOT NULL,
    owner_uuid                 UUID           NOT NULL,
    currency_code              VARCHAR(3)     NOT NULL,
    amount                     NUMERIC(18, 2) NOT NULL,
    fee_amount                 NUMERIC(18, 2) NOT NULL DEFAULT 0,
    net_amount                 NUMERIC(18, 2) NOT NULL,
    payout_method_uuid         UUID           NOT NULL,
    msisdn                     VARCHAR(12)    NOT NULL,
    status                     VARCHAR(32)    NOT NULL,
    idempotency_key            VARCHAR(80)    NOT NULL,
    requested_by_user_uuid     UUID,
    requester_name             VARCHAR(160),
    review_flags               VARCHAR(200),
    confirmed_at               TIMESTAMPTZ,
    second_approver_user_uuid  UUID,
    second_approved_at         TIMESTAMPTZ,
    reviewed_by_user_uuid      UUID,
    reviewed_at                TIMESTAMPTZ,
    rejection_reason           TEXT,
    sent_at                    TIMESTAMPTZ,
    gateway_payout_uuid        VARCHAR(64),
    mpesa_receipt              VARCHAR(40),
    mpesa_recipient_name       VARCHAR(160),
    mpesa_charges              NUMERIC(18, 2),
    failure_reason             TEXT,
    completed_at               TIMESTAMPTZ,
    last_checked_at            TIMESTAMPTZ,
    status_checks              INTEGER        NOT NULL DEFAULT 0,
    created_date               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 VARCHAR(50)    NOT NULL,
    updated_date               TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 VARCHAR(50),
    CONSTRAINT chk_withdrawal_owner_type CHECK (owner_type IN ('USER', 'ORGANISATION')),
    CONSTRAINT chk_withdrawal_status CHECK (status IN (
        'AWAITING_CONFIRMATION', 'AWAITING_SECOND_APPROVAL', 'NEEDS_REVIEW', 'SENDING', 'PROCESSING',
        'PAID', 'FAILED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_withdrawal_amounts CHECK (amount > 0 AND fee_amount >= 0 AND net_amount > 0
        AND net_amount = amount - fee_amount),
    CONSTRAINT chk_withdrawal_msisdn CHECK (msisdn ~ '^254[17][0-9]{8}$'),
    CONSTRAINT uq_withdrawal_idempotency UNIQUE (owner_type, owner_uuid, idempotency_key)
);

CREATE INDEX idx_withdrawals_owner ON withdrawals (owner_type, owner_uuid, id DESC);
CREATE INDEX idx_withdrawals_status ON withdrawals (status);
-- One withdrawal in flight per payout number at a time.
CREATE UNIQUE INDEX uq_withdrawals_open_per_method ON withdrawals (payout_method_uuid)
    WHERE status IN ('AWAITING_CONFIRMATION', 'AWAITING_SECOND_APPROVAL', 'NEEDS_REVIEW', 'SENDING', 'PROCESSING');

-- A payout number whose M-Pesa name did not match its owner sends later withdrawals to review.
ALTER TABLE payout_methods ADD COLUMN name_mismatch BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE wallet_audit_events DROP CONSTRAINT chk_wallet_audit_event_type;
ALTER TABLE wallet_audit_events ADD CONSTRAINT chk_wallet_audit_event_type CHECK (event_type IN (
    'DEPOSIT_RECORDED', 'SALE_SHARE_CREDITED', 'PLATFORM_FEE_CHARGED',
    'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'SALE_SHARE_REVERSED',
    'PAYOUT_METHOD_ADDED', 'PAYOUT_METHOD_VERIFIED', 'PAYOUT_METHOD_REMOVED', 'PAYOUT_METHOD_DEFAULT_CHANGED',
    'PAYOUT_APPROVER_ADDED', 'PAYOUT_APPROVER_REMOVED',
    'WITHDRAWAL_REQUESTED', 'WITHDRAWAL_CONFIRMED', 'WITHDRAWAL_SECOND_APPROVED', 'WITHDRAWAL_APPROVED',
    'WITHDRAWAL_REJECTED', 'WITHDRAWAL_PAID', 'WITHDRAWAL_FAILED', 'WITHDRAWAL_REVERSED',
    'WITHDRAWAL_CANCELLED', 'PAYOUT_FEE_CHARGED', 'PAYOUT_SETTINGS_CHANGED'));
