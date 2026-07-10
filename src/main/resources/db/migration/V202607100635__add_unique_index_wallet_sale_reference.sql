-- Guarantee a sale is credited to a wallet at most once, even under concurrent
-- duplicate order-capture deliveries. Scoped to SALE so deposits/transfers
-- (which may reuse or omit references) are unaffected.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_wallet_txn_sale_reference
    ON user_wallet_transactions (reference)
    WHERE transaction_type = 'SALE' AND reference IS NOT NULL;
