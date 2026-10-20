-- The creator's net share of each settled line, so earnings can be summed per course without reading wallets.
ALTER TABLE commerce_purchase_item
    ADD COLUMN creator_credited_amount NUMERIC(18, 4);

-- Lines settled so far: the creator's SALE credit for the line minus its platform fee, if any.
UPDATE commerce_purchase_item i
SET creator_credited_amount = s.amount - COALESCE(f.amount, 0)
FROM commerce_purchase p,
     user_wallet_transactions s
         LEFT JOIN user_wallet_transactions f
                   ON f.reference = s.reference || ':fee' AND f.transaction_type = 'PLATFORM_FEE'
WHERE p.id = i.purchase_id
  AND s.reference = p.order_id || ':' || i.line_item_id || ':creator'
  AND s.transaction_type = 'SALE';
