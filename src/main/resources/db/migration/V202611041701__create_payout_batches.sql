-- Withdrawals are paid in admin-approved batches. One admin builds and submits a batch; a different
-- admin approves it against the payout float, and only then are its rows handed to M-Pesa.

ALTER TABLE wallet_payout_settings ADD COLUMN require_admin_approval BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE payout_batches
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    status                VARCHAR(32)    NOT NULL,
    created_by_user_uuid  UUID,
    created_by_name       VARCHAR(160),
    submitted_by_user_uuid UUID,
    submitted_by_name     VARCHAR(160),
    submitted_at          TIMESTAMPTZ,
    approved_by_user_uuid UUID,
    approved_by_name      VARCHAR(160),
    approved_at           TIMESTAMPTZ,
    rejected_by_user_uuid UUID,
    rejected_by_name      VARCHAR(160),
    rejected_at           TIMESTAMPTZ,
    rejection_reason      TEXT,
    row_count             INTEGER        NOT NULL DEFAULT 0,
    total_amount          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    float_at_approval     NUMERIC(18, 2),
    float_override        BOOLEAN        NOT NULL DEFAULT FALSE,
    released_at           TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    created_date          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50)    NOT NULL,
    updated_date          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    CONSTRAINT chk_payout_batch_status CHECK (status IN (
        'DRAFT', 'AWAITING_APPROVAL', 'RELEASED', 'COMPLETED', 'PARTLY_FAILED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_payout_batch_totals CHECK (row_count >= 0 AND total_amount >= 0),
    -- Maker-checker: whoever built or submitted a batch cannot approve it.
    CONSTRAINT chk_payout_batch_checker CHECK (approved_by_user_uuid IS NULL
        OR (approved_by_user_uuid IS DISTINCT FROM created_by_user_uuid
            AND approved_by_user_uuid IS DISTINCT FROM submitted_by_user_uuid))
);

CREATE INDEX idx_payout_batches_status ON payout_batches (status, id DESC);

-- A withdrawal points at the one batch it is in, so it can never sit in two at once. Rows stay
-- linked to a finished batch for its history; rejecting or cancelling a batch unlinks them.
ALTER TABLE withdrawals ADD COLUMN payout_batch_uuid UUID REFERENCES payout_batches (uuid);
ALTER TABLE withdrawals ADD CONSTRAINT chk_withdrawal_batch_status CHECK (payout_batch_uuid IS NULL
    OR status IN ('NEEDS_REVIEW', 'SENDING', 'PROCESSING', 'PAID', 'FAILED'));
CREATE INDEX idx_withdrawals_payout_batch ON withdrawals (payout_batch_uuid, id) WHERE payout_batch_uuid IS NOT NULL;

ALTER TABLE wallet_audit_events DROP CONSTRAINT chk_wallet_audit_event_type;
ALTER TABLE wallet_audit_events ADD CONSTRAINT chk_wallet_audit_event_type CHECK (event_type IN (
    'DEPOSIT_RECORDED', 'SALE_SHARE_CREDITED', 'PLATFORM_FEE_CHARGED',
    'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'SALE_SHARE_REVERSED',
    'PAYOUT_METHOD_ADDED', 'PAYOUT_METHOD_VERIFIED', 'PAYOUT_METHOD_REMOVED', 'PAYOUT_METHOD_DEFAULT_CHANGED',
    'PAYOUT_APPROVER_ADDED', 'PAYOUT_APPROVER_REMOVED',
    'WITHDRAWAL_REQUESTED', 'WITHDRAWAL_CONFIRMED', 'WITHDRAWAL_HELD', 'WITHDRAWAL_SECOND_APPROVED',
    'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED', 'WITHDRAWAL_PAID', 'WITHDRAWAL_FAILED', 'WITHDRAWAL_REVERSED',
    'WITHDRAWAL_CANCELLED', 'PAYOUT_FEE_CHARGED', 'PAYOUT_SETTINGS_CHANGED',
    'PAYOUT_BATCH_ROW_ADDED', 'PAYOUT_BATCH_ROW_REMOVED', 'PAYOUT_BATCH_SUBMITTED', 'PAYOUT_BATCH_RETURNED_TO_DRAFT',
    'PAYOUT_BATCH_APPROVED', 'PAYOUT_BATCH_REJECTED', 'PAYOUT_BATCH_CANCELLED', 'PAYOUT_BATCH_COMPLETED'));
