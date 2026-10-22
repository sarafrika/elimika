-- One immutable row per thing that happened to a wallet: money moving now, payout details and
-- withdrawals later. The wallet's Audit tab reads only this table.
CREATE TABLE wallet_audit_events
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    owner_type      VARCHAR(20)  NOT NULL,
    owner_uuid      UUID         NOT NULL,
    currency_code   VARCHAR(3),
    event_type      VARCHAR(48)  NOT NULL,
    actor_user_uuid UUID,
    reference       VARCHAR(128),
    reason          TEXT,
    previous_values JSONB        NOT NULL DEFAULT '{}'::jsonb,
    updated_values  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_date    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)  NOT NULL,
    updated_date    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    CONSTRAINT chk_wallet_audit_owner_type CHECK (owner_type IN ('USER', 'ORGANISATION')),
    CONSTRAINT chk_wallet_audit_event_type CHECK (event_type IN (
        'DEPOSIT_RECORDED', 'SALE_SHARE_CREDITED', 'PLATFORM_FEE_CHARGED',
        'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'SALE_SHARE_REVERSED'))
);

CREATE INDEX idx_wallet_audit_owner ON wallet_audit_events (owner_type, owner_uuid, id DESC);

CREATE OR REPLACE FUNCTION reject_wallet_audit_change() RETURNS trigger AS
$$
BEGIN
    RAISE EXCEPTION 'wallet_audit_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wallet_audit_events_append_only
    BEFORE UPDATE OR DELETE ON wallet_audit_events
    FOR EACH ROW EXECUTE FUNCTION reject_wallet_audit_change();

-- Every wallet row recorded so far becomes its audit event, at the time it happened.
INSERT INTO wallet_audit_events (owner_type, owner_uuid, currency_code, event_type, reference, reason,
                                 previous_values, updated_values, created_date, created_by, updated_date)
SELECT w.owner_type,
       w.user_uuid,
       t.currency_code,
       CASE t.transaction_type
           WHEN 'DEPOSIT' THEN 'DEPOSIT_RECORDED'
           WHEN 'SALE' THEN 'SALE_SHARE_CREDITED'
           WHEN 'PLATFORM_FEE' THEN 'PLATFORM_FEE_CHARGED'
           WHEN 'TRANSFER_OUT' THEN 'TRANSFER_SENT'
           WHEN 'TRANSFER_IN' THEN 'TRANSFER_RECEIVED'
           WHEN 'SALE_REVERSAL' THEN 'SALE_SHARE_REVERSED'
       END,
       t.reference,
       t.description,
       jsonb_build_object('balance_amount', t.balance_before),
       jsonb_build_object('balance_amount', t.balance_after, 'amount', t.amount,
                          'transaction_type', t.transaction_type, 'transaction_uuid', t.uuid),
       t.created_date,
       t.created_by,
       t.created_date
FROM user_wallet_transactions t
         JOIN user_wallets w ON w.id = t.wallet_id
WHERE t.transaction_type IN ('DEPOSIT', 'SALE', 'PLATFORM_FEE', 'TRANSFER_OUT', 'TRANSFER_IN', 'SALE_REVERSAL')
ORDER BY t.id;
