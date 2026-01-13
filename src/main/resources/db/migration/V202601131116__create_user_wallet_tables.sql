CREATE TABLE IF NOT EXISTS user_wallets
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_uuid      UUID         NOT NULL,
    currency_code  VARCHAR(3)   NOT NULL,
    balance_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    created_date   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50)  NOT NULL,
    updated_date   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(50),
    CONSTRAINT fk_user_wallet_user FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_user_wallet_currency FOREIGN KEY (currency_code) REFERENCES currencies (code),
    CONSTRAINT uq_user_wallet_user_currency UNIQUE (user_uuid, currency_code),
    CONSTRAINT chk_user_wallet_balance_non_negative CHECK (balance_amount >= 0),
    CONSTRAINT chk_user_wallet_currency_length CHECK (char_length(currency_code) = 3)
);

CREATE INDEX idx_user_wallet_user_uuid ON user_wallets (user_uuid);

CREATE TABLE IF NOT EXISTS user_wallet_transactions
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    wallet_id            BIGINT       NOT NULL,
    transaction_type     VARCHAR(32)  NOT NULL,
    amount               NUMERIC(18,4) NOT NULL,
    currency_code        VARCHAR(3)   NOT NULL,
    balance_before       NUMERIC(18,4) NOT NULL,
    balance_after        NUMERIC(18,4) NOT NULL,
    reference            VARCHAR(128),
    description          TEXT,
    transfer_reference   UUID,
    counterparty_user_uuid UUID,
    created_date         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(50)  NOT NULL,
    updated_date         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(50),
    CONSTRAINT fk_user_wallet_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES user_wallets (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_wallet_transaction_currency FOREIGN KEY (currency_code) REFERENCES currencies (code),
    CONSTRAINT chk_user_wallet_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_user_wallet_transaction_balance_non_negative CHECK (balance_before >= 0 AND balance_after >= 0),
    CONSTRAINT chk_user_wallet_transaction_currency_length CHECK (char_length(currency_code) = 3)
);

CREATE INDEX idx_user_wallet_transaction_wallet_id ON user_wallet_transactions (wallet_id);
CREATE INDEX idx_user_wallet_transaction_type ON user_wallet_transactions (transaction_type);
CREATE INDEX idx_user_wallet_transaction_transfer_ref ON user_wallet_transactions (transfer_reference);
