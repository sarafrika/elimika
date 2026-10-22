-- Where a wallet's withdrawals go (M-Pesa numbers first), and which organisation admins may handle an
-- organisation's payouts. A number is verified by a one-time code sent to it, then held before use.
CREATE TABLE payout_methods
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    owner_type            VARCHAR(20)  NOT NULL,
    owner_uuid            UUID         NOT NULL,
    method_type           VARCHAR(20)  NOT NULL DEFAULT 'MPESA',
    msisdn                VARCHAR(12)  NOT NULL,
    label                 VARCHAR(40),
    status                VARCHAR(24)  NOT NULL,
    is_default            BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at           TIMESTAMPTZ,
    unlock_at             TIMESTAMPTZ,
    removed_at            TIMESTAMPTZ,
    mpesa_registered_name VARCHAR(120),
    added_by_user_uuid    UUID,
    created_date          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50)  NOT NULL,
    updated_date          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    CONSTRAINT chk_payout_method_owner_type CHECK (owner_type IN ('USER', 'ORGANISATION')),
    CONSTRAINT chk_payout_method_type CHECK (method_type IN ('MPESA')),
    CONSTRAINT chk_payout_method_status CHECK (status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REMOVED')),
    CONSTRAINT chk_payout_method_msisdn CHECK (msisdn ~ '^254[17][0-9]{8}$'),
    CONSTRAINT chk_payout_method_default_verified CHECK (NOT is_default OR status = 'VERIFIED')
);

CREATE INDEX idx_payout_methods_owner ON payout_methods (owner_type, owner_uuid);
-- One live entry per number per owner, and at most one default per owner.
CREATE UNIQUE INDEX uq_payout_methods_owner_msisdn ON payout_methods (owner_type, owner_uuid, msisdn)
    WHERE status <> 'REMOVED';
CREATE UNIQUE INDEX uq_payout_methods_owner_default ON payout_methods (owner_type, owner_uuid)
    WHERE is_default;

CREATE TABLE payout_approvers
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid  UUID         NOT NULL,
    user_uuid          UUID         NOT NULL,
    added_by_user_uuid UUID,
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50)  NOT NULL,
    updated_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT uq_payout_approver UNIQUE (organisation_uuid, user_uuid)
);

ALTER TABLE wallet_audit_events DROP CONSTRAINT chk_wallet_audit_event_type;
ALTER TABLE wallet_audit_events ADD CONSTRAINT chk_wallet_audit_event_type CHECK (event_type IN (
    'DEPOSIT_RECORDED', 'SALE_SHARE_CREDITED', 'PLATFORM_FEE_CHARGED',
    'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'SALE_SHARE_REVERSED',
    'PAYOUT_METHOD_ADDED', 'PAYOUT_METHOD_VERIFIED', 'PAYOUT_METHOD_REMOVED', 'PAYOUT_METHOD_DEFAULT_CHANGED',
    'PAYOUT_APPROVER_ADDED', 'PAYOUT_APPROVER_REMOVED'));
