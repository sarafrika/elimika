-- A wallet belongs to a user or an organisation. user_uuid keeps its name (history refers to it) and now
-- holds the owner's uuid of either kind, so it can no longer reference users.
ALTER TABLE user_wallets
    ADD COLUMN owner_type VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE user_wallets
    ADD CONSTRAINT chk_user_wallet_owner_type CHECK (owner_type IN ('USER', 'ORGANISATION'));

ALTER TABLE user_wallets DROP CONSTRAINT fk_user_wallet_user;
ALTER TABLE user_wallets DROP CONSTRAINT uq_user_wallet_user_currency;

ALTER TABLE user_wallets
    ADD CONSTRAINT uq_user_wallet_owner_currency UNIQUE (owner_type, user_uuid, currency_code);

COMMENT ON COLUMN user_wallets.user_uuid IS 'Owner uuid: a users.uuid or an organisation uuid, per owner_type';

-- A platform fee is charged at most once per settled share, like the sale it belongs to.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_wallet_txn_fee_reference
    ON user_wallet_transactions (reference)
    WHERE transaction_type = 'PLATFORM_FEE' AND reference IS NOT NULL;
