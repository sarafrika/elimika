-- Each settled purchase line already carries its fee, credit, retention and the creator's credit. These
-- columns add what explains the split per leg, written in the same settlement: who each leg paid, its gross,
-- fee and net, the creator's percentage, and the fee rule that priced it. creator_credited_amount stays the
-- creator's net.

ALTER TABLE commerce_purchase_item
    ADD COLUMN creator_percentage      NUMERIC(7, 4),
    ADD COLUMN creator_owner_type      VARCHAR(20),
    ADD COLUMN creator_owner_uuid      UUID,
    ADD COLUMN creator_status          VARCHAR(20),
    ADD COLUMN creator_gross           NUMERIC(18, 4),
    ADD COLUMN creator_fee             NUMERIC(18, 4),
    ADD COLUMN delivery_kind           VARCHAR(20),
    ADD COLUMN delivery_owner_type     VARCHAR(20),
    ADD COLUMN delivery_owner_uuid     UUID,
    ADD COLUMN delivery_status         VARCHAR(20),
    ADD COLUMN delivery_gross          NUMERIC(18, 4),
    ADD COLUMN delivery_fee            NUMERIC(18, 4),
    ADD COLUMN delivery_net            NUMERIC(18, 4),
    ADD COLUMN fee_rule_uuid           UUID,
    ADD COLUMN fee_rule_key            VARCHAR(128),
    ADD COLUMN fee_mode                VARCHAR(16),
    ADD COLUMN fee_rate                NUMERIC(18, 4),
    ADD COLUMN fee_rate_currency       VARCHAR(16),
    ADD COLUMN fee_waived              BOOLEAN,
    ADD COLUMN fee_discount_percentage NUMERIC(9, 4),
    ADD COLUMN fee_discount_amount     NUMERIC(18, 4),
    ADD COLUMN fee_evaluated_at        TIMESTAMPTZ,
    ADD COLUMN settled_at              TIMESTAMPTZ,
    ADD COLUMN split_reconciles        BOOLEAN,
    ADD COLUMN split_backfilled        BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE commerce_purchase_item
    ADD CONSTRAINT chk_commerce_purchase_item_split_values CHECK (
        (creator_status IS NULL OR creator_status IN ('PAID', 'NO_EARNER', 'NO_SHARE', 'NOT_SPLIT'))
            AND (delivery_status IS NULL OR delivery_status IN ('PAID', 'NO_EARNER', 'NO_SHARE', 'NOT_SPLIT'))
            AND (delivery_kind IS NULL OR delivery_kind IN ('INSTRUCTOR', 'ORGANISATION'))
            AND (creator_owner_type IS NULL OR creator_owner_type IN ('USER', 'ORGANISATION'))
            AND (delivery_owner_type IS NULL OR delivery_owner_type IN ('USER', 'ORGANISATION'))
            AND (fee_mode IS NULL OR fee_mode IN ('PERCENTAGE', 'FLAT'))
            AND COALESCE(creator_gross, 0) >= 0 AND COALESCE(creator_fee, 0) >= 0
            AND COALESCE(delivery_gross, 0) >= 0 AND COALESCE(delivery_fee, 0) >= 0
            AND COALESCE(delivery_net, 0) >= 0);

CREATE INDEX idx_commerce_purchase_item_creator_owner
    ON commerce_purchase_item (creator_owner_type, creator_owner_uuid, settled_at DESC, id DESC)
    WHERE creator_status = 'PAID';
CREATE INDEX idx_commerce_purchase_item_delivery_owner
    ON commerce_purchase_item (delivery_owner_type, delivery_owner_uuid, settled_at DESC, id DESC)
    WHERE delivery_status = 'PAID';

COMMENT ON COLUMN commerce_purchase_item.split_backfilled IS
    'Split rebuilt from wallet SALE / PLATFORM_FEE rows for a line settled before splits were recorded; percentage and fee rule unknown.';
COMMENT ON COLUMN commerce_purchase_item.split_reconciles IS
    'False when the legs did not explain the line''s fee, credit and creator credit at settlement; kept and flagged rather than refused.';

-- Lines already settled: rebuild each leg from its SALE row and that row's PLATFORM_FEE. A leg with no SALE
-- row was paid to nobody. Lines settled before fees were charged per share show their fee on the line only,
-- so their legs carry no fee and split_reconciles is false where the legs do not explain the line's fee.
WITH legs AS (
    SELECT i.id,
           cs.owner_type AS c_type, cs.owner_uuid AS c_uuid, cs.amount AS c_gross, COALESCE(cf.amount, 0) AS c_fee,
           ds.owner_type AS d_type, ds.owner_uuid AS d_uuid, ds.amount AS d_gross, COALESCE(df.amount, 0) AS d_fee,
           LEAST(cs.created_date, ds.created_date) AS first_credit_at
    FROM commerce_purchase_item i
             JOIN commerce_purchase p ON p.id = i.purchase_id
             LEFT JOIN LATERAL (
        SELECT s.amount, s.created_date, w.owner_type, w.user_uuid AS owner_uuid
        FROM user_wallet_transactions s
                 JOIN user_wallets w ON w.id = s.wallet_id
        WHERE s.reference = p.order_id || ':' || i.line_item_id || ':creator' AND s.transaction_type = 'SALE'
        ORDER BY s.id
        LIMIT 1) cs ON TRUE
             LEFT JOIN LATERAL (
        SELECT f.amount FROM user_wallet_transactions f
        WHERE f.reference = p.order_id || ':' || i.line_item_id || ':creator:fee' AND f.transaction_type = 'PLATFORM_FEE'
        ORDER BY f.id
        LIMIT 1) cf ON TRUE
             LEFT JOIN LATERAL (
        SELECT s.amount, s.created_date, w.owner_type, w.user_uuid AS owner_uuid
        FROM user_wallet_transactions s
                 JOIN user_wallets w ON w.id = s.wallet_id
        WHERE s.reference = p.order_id || ':' || i.line_item_id || ':delivery' AND s.transaction_type = 'SALE'
        ORDER BY s.id
        LIMIT 1) ds ON TRUE
             LEFT JOIN LATERAL (
        SELECT f.amount FROM user_wallet_transactions f
        WHERE f.reference = p.order_id || ':' || i.line_item_id || ':delivery:fee' AND f.transaction_type = 'PLATFORM_FEE'
        ORDER BY f.id
        LIMIT 1) df ON TRUE
    WHERE i.creator_status IS NULL
      AND i.credited_amount IS NOT NULL
      AND (cs.amount IS NOT NULL OR ds.amount IS NOT NULL)
)
UPDATE commerce_purchase_item i
SET creator_owner_type  = l.c_type,
    creator_owner_uuid  = l.c_uuid,
    creator_status      = CASE WHEN l.c_uuid IS NULL THEN 'NO_EARNER' ELSE 'PAID' END,
    creator_gross       = COALESCE(l.c_gross, 0),
    creator_fee         = CASE WHEN l.c_uuid IS NULL THEN 0 ELSE l.c_fee END,
    delivery_kind       = CASE WHEN l.d_type = 'ORGANISATION' THEN 'ORGANISATION'
                               WHEN l.d_type = 'USER' THEN 'INSTRUCTOR' END,
    delivery_owner_type = l.d_type,
    delivery_owner_uuid = l.d_uuid,
    delivery_status     = CASE WHEN l.d_uuid IS NULL THEN 'NO_EARNER' ELSE 'PAID' END,
    delivery_gross      = COALESCE(l.d_gross, 0),
    delivery_fee        = CASE WHEN l.d_uuid IS NULL THEN 0 ELSE l.d_fee END,
    delivery_net        = COALESCE(l.d_gross, 0) - CASE WHEN l.d_uuid IS NULL THEN 0 ELSE l.d_fee END,
    settled_at          = l.first_credit_at,
    split_reconciles    = (COALESCE(l.c_gross, 0) - l.c_fee) + (COALESCE(l.d_gross, 0) - l.d_fee) = i.credited_amount
        AND l.c_fee + l.d_fee = COALESCE(i.platform_fee_amount, 0)
        AND COALESCE(l.c_gross, 0) - l.c_fee = COALESCE(i.creator_credited_amount, 0),
    split_backfilled    = TRUE
FROM legs l
WHERE i.id = l.id;
