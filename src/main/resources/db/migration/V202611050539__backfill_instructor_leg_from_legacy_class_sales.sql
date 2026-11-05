-- Lines settled before shares carried a ':creator' / ':delivery' suffix credited the instructor under the bare
-- 'order:line' reference ("Class sale earnings"). The split backfill missed those, leaving the delivery leg as
-- NO_EARNER. Fill it from that credit unless it was later reversed, then recheck that the legs explain the line.
WITH legacy AS (
    SELECT i.id,
           w.owner_type AS d_type, w.user_uuid AS d_uuid, s.amount AS d_gross, COALESCE(f.amount, 0) AS d_fee,
           s.created_date AS credited_at
    FROM commerce_purchase_item i
             JOIN commerce_purchase p ON p.id = i.purchase_id
             JOIN LATERAL (
        SELECT t.wallet_id, t.amount, t.created_date
        FROM user_wallet_transactions t
        WHERE t.reference = p.order_id || ':' || i.line_item_id AND t.transaction_type = 'SALE'
        ORDER BY t.id
        LIMIT 1) s ON TRUE
             JOIN user_wallets w ON w.id = s.wallet_id
             LEFT JOIN LATERAL (
        SELECT t.amount FROM user_wallet_transactions t
        WHERE t.reference = p.order_id || ':' || i.line_item_id || ':fee' AND t.transaction_type = 'PLATFORM_FEE'
        ORDER BY t.id
        LIMIT 1) f ON TRUE
    WHERE i.split_backfilled
      AND i.delivery_status = 'NO_EARNER'
      AND NOT EXISTS (SELECT 1 FROM user_wallet_transactions r
                      WHERE r.reference = p.order_id || ':' || i.line_item_id || ':reversal'
                        AND r.transaction_type = 'SALE_REVERSAL')
)
UPDATE commerce_purchase_item i
SET delivery_kind       = CASE WHEN l.d_type = 'ORGANISATION' THEN 'ORGANISATION' ELSE 'INSTRUCTOR' END,
    delivery_owner_type = l.d_type,
    delivery_owner_uuid = l.d_uuid,
    delivery_status     = 'PAID',
    delivery_gross      = l.d_gross,
    delivery_fee        = l.d_fee,
    delivery_net        = l.d_gross - l.d_fee,
    settled_at          = LEAST(i.settled_at, l.credited_at),
    split_reconciles    = (COALESCE(i.creator_gross, 0) - COALESCE(i.creator_fee, 0)) + (l.d_gross - l.d_fee) = i.credited_amount
        AND COALESCE(i.creator_fee, 0) + l.d_fee = COALESCE(i.platform_fee_amount, 0)
        AND COALESCE(i.creator_gross, 0) - COALESCE(i.creator_fee, 0) = COALESCE(i.creator_credited_amount, 0)
FROM legacy l
WHERE i.id = l.id;
