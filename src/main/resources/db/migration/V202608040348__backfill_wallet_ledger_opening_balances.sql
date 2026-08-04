-- Opening-balance backfill: put the ledger in agreement with user_wallets on day one.
--
-- Every existing wallet gets a USER/EARNINGS liability account and, when it holds money, one
-- opening transaction crediting that account and debiting PLATFORM_UNALLOCATED_REVENUE. Balances
-- pre-date the ledger, so there is no better contra account available in the phase-1 chart; a
-- dedicated opening-balance/suspense account would be cleaner and is worth adding when the purse
-- taxonomy is expanded.
--
-- Every statement below is re-runnable. Accounts are guarded by their identity index, opening
-- transactions by their idempotency key, entries by "this transaction already has entries", and
-- the cached balances are *recomputed from the entries* rather than incremented - so running this
-- twice, or running it long after live dual-writes have started, cannot double-count.
--
-- The whole backfill is one DO block so it is a single statement: the deferred zero-sum trigger
-- fires once, at the end, no matter what autocommit mode the runner happens to be in.
DO
$$
    BEGIN
        -- 1. A liability account per wallet. The platform owes this money to the user.
        INSERT INTO ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, status, created_by)
        SELECT DISTINCT 'USER', w.user_uuid, 'LIABILITY', 'EARNINGS', w.currency_code, 'ACTIVE', 'SYSTEM'
        FROM user_wallets w
        WHERE NOT EXISTS (SELECT 1
                          FROM ledger_accounts la
                          WHERE la.owner_type = 'USER'
                            AND la.owner_uuid = w.user_uuid
                            AND la.purse = 'EARNINGS'
                            AND la.currency_code = w.currency_code);

        -- 2. The contra account, in every currency a wallet actually uses.
        INSERT INTO ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, status, created_by)
        -- NULL has to be cast: SELECT DISTINCT resolves an untyped null to text, and owner_uuid is
        -- a uuid column.
        SELECT DISTINCT 'PLATFORM', NULL::UUID, 'REVENUE', 'PLATFORM_UNALLOCATED_REVENUE', w.currency_code, 'ACTIVE',
                        'SYSTEM'
        FROM user_wallets w
        WHERE NOT EXISTS (SELECT 1
                          FROM ledger_accounts la
                          WHERE la.owner_type = 'PLATFORM'
                            AND la.owner_uuid IS NULL
                            AND la.purse = 'PLATFORM_UNALLOCATED_REVENUE'
                            AND la.currency_code = w.currency_code);

        -- 3. One opening transaction per funded wallet, keyed on the wallet so a second run is a
        --    no-op on the unique idempotency key.
        INSERT INTO ledger_transactions (idempotency_key, occurred_at, description, cause_type, cause_uuid, created_by)
        SELECT 'wallet-opening:' || w.uuid,
               w.created_date,
               'Opening balance carried over from user_wallets',
               'WALLET_OPENING_BALANCE',
               w.uuid,
               'SYSTEM'
        FROM user_wallets w
        WHERE w.balance_amount <> 0
          AND NOT EXISTS (SELECT 1
                          FROM ledger_transactions t
                          WHERE t.idempotency_key = 'wallet-opening:' || w.uuid);

        -- 4. Both legs in one statement. Splitting them would make the second statement see the
        --    first one's rows and skip - and would leave a half-written transaction behind.
        INSERT INTO ledger_entries (transaction_uuid, account_uuid, direction, amount, currency_code, created_by)
        SELECT t.uuid, leg.account_uuid, leg.direction, w.balance_amount, w.currency_code, 'SYSTEM'
        FROM user_wallets w
                 JOIN ledger_transactions t ON t.idempotency_key = 'wallet-opening:' || w.uuid
                 JOIN ledger_accounts ua ON ua.owner_type = 'USER'
            AND ua.owner_uuid = w.user_uuid
            AND ua.purse = 'EARNINGS'
            AND ua.currency_code = w.currency_code
                 JOIN ledger_accounts pa ON pa.owner_type = 'PLATFORM'
            AND pa.owner_uuid IS NULL
            AND pa.purse = 'PLATFORM_UNALLOCATED_REVENUE'
            AND pa.currency_code = w.currency_code
                 CROSS JOIN LATERAL (VALUES (ua.uuid, 'CREDIT'::VARCHAR),
                                            (pa.uuid, 'DEBIT'::VARCHAR)) AS leg(account_uuid, direction)
        WHERE w.balance_amount <> 0
          AND NOT EXISTS (SELECT 1 FROM ledger_entries e WHERE e.transaction_uuid = t.uuid);

        -- 5. Rebuild every cached balance from the entries. Recomputing rather than incrementing is
        --    what makes this step safe to repeat, and it is also the rebuild path the cache claims
        --    to have.
        INSERT INTO ledger_account_balances (account_uuid, posted_amount, pending_amount, created_by)
        SELECT a.uuid, COALESCE(d.net, 0), 0, 'SYSTEM'
        FROM ledger_accounts a
                 LEFT JOIN LATERAL (
            SELECT sum(CASE
                           WHEN a.account_type IN ('ASSET', 'EXPENSE')
                               THEN CASE WHEN e.direction = 'DEBIT' THEN e.amount ELSE -e.amount END
                           ELSE CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END
                           END) AS net
            FROM ledger_entries e
            WHERE e.account_uuid = a.uuid
            ) d ON TRUE
        ON CONFLICT (account_uuid)
            DO UPDATE SET posted_amount = EXCLUDED.posted_amount,
                          version       = ledger_account_balances.version + 1,
                          updated_date  = CURRENT_TIMESTAMP;
    END
$$;
