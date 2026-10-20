-- A sale is reversed at most once; the reversal carries the sale's reference with a :reversal suffix.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_wallet_txn_reversal_reference
    ON user_wallet_transactions (reference)
    WHERE transaction_type = 'SALE_REVERSAL' AND reference IS NOT NULL;
