-- Double-entry ledger for the wallet module.
--
-- user_wallets remains authoritative for this phase; the ledger is dual-written beside it so the
-- two can be compared. The point of the ledger is the invariant: every transaction's entries must
-- net to zero per currency, enforced by the database rather than by whoever remembers to.
--
-- The ledger is deliberately policy-free. It knows accounts, directions and amounts. It knows
-- nothing about courses, organisations, fee percentages or revenue shares.

-- ---------------------------------------------------------------------------------------------
-- Accounts
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ledger_accounts
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    owner_type    VARCHAR(32) NOT NULL,
    owner_uuid    UUID,
    account_type  VARCHAR(32) NOT NULL,
    purse         VARCHAR(64) NOT NULL,
    currency_code VARCHAR(3)  NOT NULL,
    status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_date  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_date  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    CONSTRAINT fk_ledger_account_currency FOREIGN KEY (currency_code) REFERENCES currencies (code),
    CONSTRAINT chk_ledger_account_owner_type CHECK (owner_type IN ('USER', 'ORGANISATION', 'PLATFORM')),
    CONSTRAINT chk_ledger_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT chk_ledger_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    -- Phase 1 purse taxonomy. Party purses are EARNINGS only; the remaining values name the
    -- platform's internal accounts, because purse is what distinguishes accounts under one owner
    -- and every platform account shares owner_type = 'PLATFORM' with a null owner.
    CONSTRAINT chk_ledger_account_purse CHECK (purse IN (
        'EARNINGS',
        'PLATFORM_CASH_MPESA',
        'PLATFORM_FEE_REVENUE',
        'PLATFORM_UNALLOCATED_REVENUE',
        'PAYOUTS_IN_FLIGHT',
        'PAYOUT_FEES'
    )),
    -- Only the platform's own accounts may be ownerless.
    CONSTRAINT chk_ledger_account_owner_uuid CHECK (owner_type = 'PLATFORM' OR owner_uuid IS NOT NULL),
    CONSTRAINT chk_ledger_account_currency_length CHECK (char_length(currency_code) = 3)
);

-- A plain UNIQUE (owner_type, owner_uuid, purse, currency_code) would not deduplicate the platform
-- accounts at all: owner_uuid is null for every one of them, and in SQL null is distinct from null,
-- so the constraint would happily admit five copies of PLATFORM_CASH_MPESA. COALESCE onto the nil
-- UUID makes the identity total. (UNIQUE NULLS NOT DISTINCT would also work but needs PG 15+.)
CREATE UNIQUE INDEX uq_ledger_account_identity
    ON ledger_accounts (owner_type, COALESCE(owner_uuid, '00000000-0000-0000-0000-000000000000'::uuid), purse,
                        currency_code);

CREATE INDEX idx_ledger_account_owner ON ledger_accounts (owner_type, owner_uuid);

-- ---------------------------------------------------------------------------------------------
-- Transactions (immutable)
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ledger_transactions
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(160) NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description     TEXT,
    cause_type      VARCHAR(64),
    cause_uuid      UUID,
    created_date    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)  NOT NULL,
    updated_date    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    CONSTRAINT uq_ledger_transaction_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_ledger_transaction_cause ON ledger_transactions (cause_type, cause_uuid);
CREATE INDEX idx_ledger_transaction_occurred_at ON ledger_transactions (occurred_at);

-- ---------------------------------------------------------------------------------------------
-- Entries (immutable)
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ledger_entries
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    transaction_uuid UUID          NOT NULL,
    account_uuid     UUID          NOT NULL,
    direction        VARCHAR(8)    NOT NULL,
    amount           NUMERIC(18, 4) NOT NULL,
    currency_code    VARCHAR(3)    NOT NULL,
    created_date     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50)   NOT NULL,
    updated_date     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(50),
    CONSTRAINT fk_ledger_entry_transaction FOREIGN KEY (transaction_uuid) REFERENCES ledger_transactions (uuid),
    CONSTRAINT fk_ledger_entry_account FOREIGN KEY (account_uuid) REFERENCES ledger_accounts (uuid),
    CONSTRAINT fk_ledger_entry_currency FOREIGN KEY (currency_code) REFERENCES currencies (code),
    CONSTRAINT chk_ledger_entry_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    -- Amounts are unsigned; direction carries the sign. A zero entry is noise, not accounting.
    CONSTRAINT chk_ledger_entry_amount CHECK (amount > 0),
    CONSTRAINT chk_ledger_entry_currency_length CHECK (char_length(currency_code) = 3)
);

CREATE INDEX idx_ledger_entry_transaction ON ledger_entries (transaction_uuid);
CREATE INDEX idx_ledger_entry_account ON ledger_entries (account_uuid);

-- ---------------------------------------------------------------------------------------------
-- Cached balances (derived, rebuildable - the entries are the truth)
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ledger_account_balances
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    account_uuid   UUID          NOT NULL,
    posted_amount  NUMERIC(18, 4) NOT NULL DEFAULT 0,
    pending_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_date   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50)   NOT NULL,
    updated_date   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(50),
    CONSTRAINT fk_ledger_account_balance_account FOREIGN KEY (account_uuid) REFERENCES ledger_accounts (uuid) ON DELETE CASCADE,
    CONSTRAINT uq_ledger_account_balance_account UNIQUE (account_uuid)
);

-- ---------------------------------------------------------------------------------------------
-- The zero-sum invariant
-- ---------------------------------------------------------------------------------------------
-- Debits are positive, credits negative; a transaction's entries must net to zero in every
-- currency it touches, and a transaction with fewer than two entries cannot be balanced in any
-- meaningful sense (an empty transaction sums to zero vacuously, which is exactly the hole an
-- entries-only check would leave open).
CREATE OR REPLACE FUNCTION ledger_assert_balanced(p_transaction_uuid UUID) RETURNS VOID AS
$$
DECLARE
    v_entry_count BIGINT;
    v_offender    RECORD;
BEGIN
    -- The transaction itself may have been removed within the same transaction (test fixtures,
    -- rollback to savepoint); there is nothing left to assert about it.
    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE uuid = p_transaction_uuid) THEN
        RETURN;
    END IF;

    SELECT count(*) INTO v_entry_count FROM ledger_entries WHERE transaction_uuid = p_transaction_uuid;
    IF v_entry_count < 2 THEN
        RAISE EXCEPTION 'Ledger transaction % has % entries; a balanced transaction needs at least two',
            p_transaction_uuid, v_entry_count USING ERRCODE = '23514';
    END IF;

    FOR v_offender IN
        SELECT currency_code,
               sum(CASE WHEN direction = 'DEBIT' THEN amount ELSE -amount END) AS net
        FROM ledger_entries
        WHERE transaction_uuid = p_transaction_uuid
        GROUP BY currency_code
        HAVING sum(CASE WHEN direction = 'DEBIT' THEN amount ELSE -amount END) <> 0
        LOOP
            RAISE EXCEPTION 'Ledger transaction % does not balance in %: debits minus credits = %',
                p_transaction_uuid, v_offender.currency_code, v_offender.net USING ERRCODE = '23514';
        END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ledger_entries_assert_balanced() RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM ledger_assert_balanced(OLD.transaction_uuid);
    ELSE
        PERFORM ledger_assert_balanced(NEW.transaction_uuid);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ledger_transactions_assert_balanced() RETURNS TRIGGER AS
$$
BEGIN
    PERFORM ledger_assert_balanced(NEW.uuid);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- DEFERRABLE INITIALLY DEFERRED is the whole trick. Entries arrive one INSERT at a time (Hibernate
-- has no way to write them as a single statement), so an immediate check would reject the very
-- first leg of every transaction ever written. A deferred constraint trigger runs at COMMIT, when
-- the transaction is complete and the question "does this balance?" is finally answerable.
--
-- The alternative - a plain CHECK constraint - cannot express this at all: CHECK sees one row and
-- can neither aggregate across siblings nor be deferred. Enforcing it in application code was the
-- other option and is the one being replaced: it is exactly the arrangement that let user_wallets
-- drift silently.
CREATE CONSTRAINT TRIGGER trg_ledger_entries_balanced
    AFTER INSERT OR UPDATE OR DELETE
    ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION ledger_entries_assert_balanced();

-- Catches the case the entries trigger structurally cannot: a transaction row with no entries at
-- all, which never fires a trigger on ledger_entries.
CREATE CONSTRAINT TRIGGER trg_ledger_transactions_balanced
    AFTER INSERT
    ON ledger_transactions
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION ledger_transactions_assert_balanced();

-- ---------------------------------------------------------------------------------------------
-- Immutability
-- ---------------------------------------------------------------------------------------------
-- Corrections are posted as new, reversing transactions. Nothing is edited away, so the entries
-- remain evidence rather than a current opinion.
CREATE OR REPLACE FUNCTION ledger_reject_mutation() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'Ledger table % is append-only; % is not permitted - post a reversing transaction instead',
        TG_TABLE_NAME, TG_OP USING ERRCODE = '23514';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_transactions_immutable
    BEFORE UPDATE OR DELETE
    ON ledger_transactions
    FOR EACH ROW
EXECUTE FUNCTION ledger_reject_mutation();

CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE
    ON ledger_entries
    FOR EACH ROW
EXECUTE FUNCTION ledger_reject_mutation();

-- ---------------------------------------------------------------------------------------------
-- Internal platform accounts
-- ---------------------------------------------------------------------------------------------
-- Seeded for the platform's default currency. Accounts in other currencies are created on demand
-- (see LedgerServiceImpl#getOrCreateAccountUuid) and by the opening-balance backfill.
INSERT INTO ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, status, created_by)
SELECT 'PLATFORM', NULL, seed.account_type, seed.purse, c.code, 'ACTIVE', 'SYSTEM'
FROM (VALUES ('ASSET', 'PLATFORM_CASH_MPESA'),
             ('REVENUE', 'PLATFORM_FEE_REVENUE'),
             ('REVENUE', 'PLATFORM_UNALLOCATED_REVENUE'),
             ('LIABILITY', 'PAYOUTS_IN_FLIGHT'),
             ('EXPENSE', 'PAYOUT_FEES')) AS seed(account_type, purse)
         CROSS JOIN (SELECT code FROM currencies WHERE is_default) c
WHERE NOT EXISTS (SELECT 1
                  FROM ledger_accounts existing
                  WHERE existing.owner_type = 'PLATFORM'
                    AND existing.owner_uuid IS NULL
                    AND existing.purse = seed.purse
                    AND existing.currency_code = c.code);
