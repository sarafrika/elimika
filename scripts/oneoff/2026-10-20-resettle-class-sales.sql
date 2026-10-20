-- One-off: pay creators what past class sales retained, and move a job class's instructor credit to the hiring
-- organisation. Preview: psql -v ON_ERROR_STOP=1 -f this.sql   Apply: add -v apply=1. Re-runs skip settled lines.
\if :{?apply}
\else
\set apply 0
\endif

BEGIN;

CREATE TEMP TABLE resettlement ON COMMIT DROP AS
SELECT p.order_id || ':' || pi.line_item_id                         AS ref,
       pi.id                                                         AS purchase_item_id,
       pi.title,
       pi.total                                                      AS gross,
       p.order_currency_code                                         AS currency,
       w.owner_type                                                  AS earlier_owner_type,
       w.user_uuid                                                   AS earlier_owner,
       t.amount                                                      AS earlier_credit,
       cc.user_uuid                                                  AS creator,
       pi.total - t.amount                                           AS creator_credit,
       CASE WHEN cd.marketplace_job_uuid IS NOT NULL THEN cd.organisation_uuid END AS hiring_organisation
FROM commerce_purchase_item pi
         JOIN commerce_purchase p ON p.id = pi.purchase_id
         JOIN class_definitions cd ON cd.uuid = pi.class_definition_uuid
         JOIN courses c ON c.uuid = cd.course_uuid
         JOIN course_creators cc ON cc.uuid = c.course_creator_uuid
         JOIN user_wallet_transactions t ON t.reference = p.order_id || ':' || pi.line_item_id
    AND t.transaction_type = 'SALE'
         JOIN user_wallets w ON w.id = t.wallet_id
WHERE lower(p.payment_status) = 'captured'
  AND pi.scope = 'CLASS'
  AND pi.total > t.amount
  AND NOT EXISTS (SELECT 1 FROM user_wallet_transactions s WHERE s.reference = p.order_id || ':' || pi.line_item_id || ':creator');

SELECT title, gross, earlier_credit, creator_credit,
       CASE WHEN hiring_organisation IS NOT NULL AND earlier_owner_type = 'USER'
            THEN 'move ' || earlier_credit || ' to organisation ' || hiring_organisation ELSE '-' END AS delivery
FROM resettlement ORDER BY ref;
SELECT count(*) AS lines, sum(creator_credit) AS to_creators,
       sum(CASE WHEN hiring_organisation IS NOT NULL AND earlier_owner_type = 'USER' THEN earlier_credit ELSE 0 END)
           AS moved_to_organisations
FROM resettlement;

-- Posts one wallet row and its two ledger legs, exactly as WalletServiceImpl does; skips a reference already used.
CREATE FUNCTION pg_temp.post(p_owner_type TEXT, p_owner UUID, p_type TEXT, p_amount NUMERIC, p_currency TEXT,
                             p_ref TEXT, p_description TEXT) RETURNS VOID AS
$$
DECLARE
    v_wallet   user_wallets%ROWTYPE;
    v_debit    BOOLEAN := p_type = 'SALE_REVERSAL';
    v_delta    NUMERIC := CASE WHEN p_type = 'SALE_REVERSAL' THEN -p_amount ELSE p_amount END;
    v_txn      UUID;
    v_earnings UUID;
    v_contra   UUID;
    v_ledger   UUID;
BEGIN
    IF EXISTS (SELECT 1 FROM user_wallet_transactions WHERE reference = p_ref) THEN
        RETURN;
    END IF;
    INSERT INTO user_wallets (owner_type, user_uuid, currency_code, balance_amount, created_by)
    VALUES (p_owner_type, p_owner, p_currency, 0, 'SYSTEM')
    ON CONFLICT (owner_type, user_uuid, currency_code) DO NOTHING;
    SELECT * INTO v_wallet FROM user_wallets
    WHERE owner_type = p_owner_type AND user_uuid = p_owner AND currency_code = p_currency FOR UPDATE;
    IF v_wallet.balance_amount + v_delta < 0 THEN
        RAISE EXCEPTION 'Wallet % holds %, cannot take %', v_wallet.uuid, v_wallet.balance_amount, p_amount;
    END IF;

    INSERT INTO user_wallet_transactions (wallet_id, transaction_type, amount, currency_code, balance_before,
                                          balance_after, reference, description, created_by)
    VALUES (v_wallet.id, p_type, p_amount, p_currency, v_wallet.balance_amount, v_wallet.balance_amount + v_delta,
            p_ref, p_description, 'SYSTEM')
    RETURNING uuid INTO v_txn;
    UPDATE user_wallets SET balance_amount = balance_amount + v_delta, updated_date = now() WHERE id = v_wallet.id;

    INSERT INTO ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, status, created_by)
    SELECT p_owner_type, p_owner, 'LIABILITY', 'EARNINGS', p_currency, 'ACTIVE', 'SYSTEM'
    WHERE NOT EXISTS (SELECT 1 FROM ledger_accounts WHERE owner_type = p_owner_type AND owner_uuid = p_owner
                        AND purse = 'EARNINGS' AND currency_code = p_currency);
    SELECT uuid INTO v_earnings FROM ledger_accounts
    WHERE owner_type = p_owner_type AND owner_uuid = p_owner AND purse = 'EARNINGS' AND currency_code = p_currency;
    SELECT uuid INTO v_contra FROM ledger_accounts
    WHERE owner_type = 'PLATFORM' AND owner_uuid IS NULL AND purse = 'PLATFORM_UNALLOCATED_REVENUE'
      AND currency_code = p_currency;

    INSERT INTO ledger_transactions (idempotency_key, occurred_at, description, cause_type, cause_uuid, created_by)
    VALUES ('wallet-txn:' || v_txn, now(), p_description, 'WALLET_TRANSACTION', v_txn, 'SYSTEM')
    RETURNING uuid INTO v_ledger;
    INSERT INTO ledger_entries (transaction_uuid, account_uuid, direction, amount, currency_code, created_by)
    VALUES (v_ledger, v_earnings, CASE WHEN v_debit THEN 'DEBIT' ELSE 'CREDIT' END, p_amount, p_currency, 'SYSTEM'),
           (v_ledger, v_contra, CASE WHEN v_debit THEN 'CREDIT' ELSE 'DEBIT' END, p_amount, p_currency, 'SYSTEM');
END;
$$ LANGUAGE plpgsql;

\if :apply
DO
$$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN SELECT * FROM resettlement ORDER BY ref LOOP
            IF r.hiring_organisation IS NOT NULL AND r.earlier_owner_type = 'USER' THEN
                PERFORM pg_temp.post(r.earlier_owner_type, r.earlier_owner, 'SALE_REVERSAL', r.earlier_credit,
                                     r.currency, r.ref || ':reversal', 'Moved to the hiring organisation - ' || r.title);
                PERFORM pg_temp.post('ORGANISATION', r.hiring_organisation, 'SALE', r.earlier_credit, r.currency,
                                     r.ref || ':delivery', 'Class sale share - ' || r.title || ' (re-settled)');
            END IF;
            PERFORM pg_temp.post('USER', r.creator, 'SALE', r.creator_credit, r.currency, r.ref || ':creator',
                                 'Class sale share - ' || r.title || ' (re-settled)');
            UPDATE commerce_purchase_item
            SET credited_amount = total - COALESCE(platform_fee_amount, 0), retained_amount = 0,
                platform_fee_amount = COALESCE(platform_fee_amount, 0)
            WHERE id = r.purchase_item_id;
        END LOOP;

        -- Rebuild cached balances from the entries, as the opening-balance backfill does.
        INSERT INTO ledger_account_balances (account_uuid, posted_amount, pending_amount, created_by)
        SELECT a.uuid, COALESCE(d.net, 0), 0, 'SYSTEM'
        FROM ledger_accounts a
                 LEFT JOIN LATERAL (SELECT sum(CASE
                                                   WHEN a.account_type IN ('ASSET', 'EXPENSE')
                                                       THEN CASE WHEN e.direction = 'DEBIT' THEN e.amount ELSE -e.amount END
                                                   ELSE CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END
                                                   END) AS net
                                    FROM ledger_entries e
                                    WHERE e.account_uuid = a.uuid) d ON TRUE
        ON CONFLICT (account_uuid) DO UPDATE SET posted_amount = EXCLUDED.posted_amount,
                                                 version       = ledger_account_balances.version + 1,
                                                 updated_date  = CURRENT_TIMESTAMP;
    END
$$;
COMMIT;
\echo 'Applied.'
\else
ROLLBACK;
\echo 'Preview only; nothing written. Re-run with -v apply=1 to apply.'
\endif
