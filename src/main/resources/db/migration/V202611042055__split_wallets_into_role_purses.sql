-- A person can earn as a course creator, deliver as an instructor and hold credit as a learner; each role now has
-- its own purse with its own balance. A wallet row is one owner, purse and currency (user_wallets.purse), every
-- wallet row belongs to the purse it moved, and the ledger keeps one liability account per purse, which the
-- existing (owner_type, owner_uuid, purse, currency_code) identity already allowed. Organisations keep their one
-- EARNINGS purse.
--
-- Existing USER wallets are split from their own history by the plan below (identical in
-- scripts/role-purse-backfill-preview.sql, which prints it without writing). The migration refuses to run if any
-- row cannot be classified, a purse would go below zero, the purses do not sum to the balance, or the ledger
-- disagrees with the wallet. Re-running it does nothing: only USER wallets without a purse are planned.

ALTER TABLE ledger_accounts DROP CONSTRAINT chk_ledger_account_purse;
ALTER TABLE ledger_accounts ADD CONSTRAINT chk_ledger_account_purse CHECK (purse IN (
    'EARNINGS',
    'COURSE_CREATOR',
    'INSTRUCTOR',
    'STUDENT',
    'PLATFORM_CASH_MPESA',
    'PLATFORM_FEE_REVENUE',
    'PLATFORM_UNALLOCATED_REVENUE',
    'PAYOUTS_IN_FLIGHT',
    'PAYOUT_FEES'
));

ALTER TABLE user_wallets ADD COLUMN IF NOT EXISTS purse VARCHAR(32);
UPDATE user_wallets SET purse = 'EARNINGS' WHERE owner_type = 'ORGANISATION' AND purse IS NULL;
ALTER TABLE user_wallets DROP CONSTRAINT IF EXISTS uq_user_wallet_owner_currency;

-- The purse a withdrawal is paid from; its debit, charge and any reversal all touch that purse's wallet row.
ALTER TABLE withdrawals ADD COLUMN IF NOT EXISTS purse VARCHAR(32);
UPDATE withdrawals SET purse = 'EARNINGS' WHERE owner_type = 'ORGANISATION' AND purse IS NULL;

ALTER TABLE wallet_audit_events DROP CONSTRAINT chk_wallet_audit_event_type;
ALTER TABLE wallet_audit_events ADD CONSTRAINT chk_wallet_audit_event_type CHECK (event_type IN (
    'DEPOSIT_RECORDED', 'SALE_SHARE_CREDITED', 'PLATFORM_FEE_CHARGED',
    'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'SALE_SHARE_REVERSED',
    'PAYOUT_METHOD_ADDED', 'PAYOUT_METHOD_VERIFIED', 'PAYOUT_METHOD_REMOVED', 'PAYOUT_METHOD_DEFAULT_CHANGED',
    'PAYOUT_APPROVER_ADDED', 'PAYOUT_APPROVER_REMOVED',
    'WITHDRAWAL_REQUESTED', 'WITHDRAWAL_CONFIRMED', 'WITHDRAWAL_HELD', 'WITHDRAWAL_SECOND_APPROVED',
    'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED', 'WITHDRAWAL_PAID', 'WITHDRAWAL_FAILED', 'WITHDRAWAL_REVERSED',
    'WITHDRAWAL_CANCELLED', 'PAYOUT_FEE_CHARGED', 'PAYOUT_SETTINGS_CHANGED',
    'PAYOUT_BATCH_ROW_ADDED', 'PAYOUT_BATCH_ROW_REMOVED', 'PAYOUT_BATCH_SUBMITTED', 'PAYOUT_BATCH_RETURNED_TO_DRAFT',
    'PAYOUT_BATCH_APPROVED', 'PAYOUT_BATCH_REJECTED', 'PAYOUT_BATCH_CANCELLED', 'PAYOUT_BATCH_COMPLETED',
    'WALLET_SPLIT_INTO_PURSES'));

-- @@role-purse-plan:begin (kept identical in the migration and scripts/role-purse-backfill-preview.sql)
-- One row per wallet row and purse it lands in. A FIFO debit spread over two purses has two portions.
CREATE TEMP TABLE role_purse_plan
(
    txn_id    BIGINT         NOT NULL,
    wallet_id BIGINT         NOT NULL,
    portion   INT            NOT NULL,
    purse     VARCHAR(32)    NOT NULL,
    amount    NUMERIC(18, 4) NOT NULL,
    rule      TEXT           NOT NULL,
    PRIMARY KEY (txn_id, portion)
) ON COMMIT DROP;

CREATE TEMP TABLE role_purse_wallets
(
    wallet_id      BIGINT PRIMARY KEY,
    owner_uuid     UUID           NOT NULL,
    currency_code  VARCHAR(3)     NOT NULL,
    balance_amount NUMERIC(18, 4) NOT NULL,
    course_creator NUMERIC(18, 4) NOT NULL,
    instructor     NUMERIC(18, 4) NOT NULL,
    student        NUMERIC(18, 4) NOT NULL,
    unclassified   NUMERIC(18, 4) NOT NULL,
    ledger_balance NUMERIC(18, 4),
    first_purse    VARCHAR(32)    NOT NULL,
    problems       TEXT[]         NOT NULL
) ON COMMIT DROP;

-- Classifies every row of every USER wallet not yet split into purses, oldest first, and replays the balance per
-- purse. Credits queue up as lots; a debit whose row does not say which purse (withdrawal, its M-Pesa charge, a
-- transfer out) takes the oldest lots first. Nothing is written outside the two temp tables.
CREATE FUNCTION pg_temp.plan_role_purses() RETURNS VOID AS
$$
DECLARE
    w          RECORD;
    t          RECORD;
    v_purse    TEXT;
    v_rule     TEXT;
    v_credit   BOOLEAN;
    v_fifo     BOOLEAN;
    v_class    UUID;
    v_course   UUID;
    v_instr    UUID;
    v_creators UUID[];
    v_base     TEXT;
    v_take     NUMERIC;
    v_left     NUMERIC;
    v_first    TEXT;
    v_ledger   NUMERIC;
    v_back     RECORD;
    v_back_sum NUMERIC;
    i          INT;
    j          INT;
    lot_purse  TEXT[];
    lot_amount NUMERIC[];
    p_purse    TEXT[];
    p_amount   NUMERIC[];
    bal        JSONB;
    problems   TEXT[];
BEGIN
    FOR w IN SELECT * FROM user_wallets uw
             WHERE uw.owner_type = 'USER' AND to_jsonb(uw) ->> 'purse' IS NULL
             ORDER BY uw.id
        LOOP
            lot_purse := ARRAY []::TEXT[];
            lot_amount := ARRAY []::NUMERIC[];
            bal := '{"COURSE_CREATOR": 0, "INSTRUCTOR": 0, "STUDENT": 0, "UNCLASSIFIED": 0}'::jsonb;
            problems := ARRAY []::TEXT[];
            v_first := NULL;

            FOR t IN SELECT * FROM user_wallet_transactions WHERE wallet_id = w.id ORDER BY created_date, id
                LOOP
                    v_purse := NULL;
                    v_rule := NULL;
                    v_fifo := FALSE;
                    v_credit := t.transaction_type IN ('SALE', 'DEPOSIT', 'TRANSFER_IN', 'WITHDRAWAL_REVERSAL');
                    p_purse := ARRAY []::TEXT[];
                    p_amount := ARRAY []::NUMERIC[];

                    IF t.transaction_type = 'SALE' THEN
                        IF t.reference LIKE '%:creator' THEN
                            v_purse := 'COURSE_CREATOR';
                            v_rule := 'sale: creator leg (:creator)';
                        ELSIF t.reference LIKE '%:delivery' THEN
                            v_purse := 'INSTRUCTOR';
                            v_rule := 'sale: delivery leg (:delivery)';
                        ELSIF t.description ~ '^Class sale earnings \(class [0-9a-fA-F-]{36}\)$' THEN
                            v_class := substring(t.description FROM '([0-9a-fA-F-]{36})')::UUID;
                            SELECT i2.user_uuid INTO v_instr
                            FROM class_definitions cd
                                     JOIN instructors i2 ON i2.uuid = cd.default_instructor_uuid
                            WHERE cd.uuid = v_class;
                            SELECT array_agg(x.user_uuid) INTO v_creators
                            FROM (SELECT cc.user_uuid
                                  FROM class_definitions cd
                                           JOIN courses c ON c.uuid = cd.course_uuid
                                           JOIN course_creators cc ON cc.uuid = c.course_creator_uuid
                                  WHERE cd.uuid = v_class
                                  UNION ALL
                                  SELECT cc.user_uuid
                                  FROM class_definitions cd
                                           JOIN training_programs tp ON tp.uuid = cd.program_uuid
                                           JOIN course_creators cc ON cc.uuid = tp.course_creator_uuid
                                  WHERE cd.uuid = v_class) x;
                            IF v_instr IS NOT DISTINCT FROM w.user_uuid THEN
                                v_purse := 'INSTRUCTOR';
                                v_rule := 'legacy class sale: owner is the class''s instructor';
                            ELSIF w.user_uuid = ANY (COALESCE(v_creators, ARRAY []::UUID[])) THEN
                                v_purse := 'COURSE_CREATOR';
                                v_rule := 'legacy class sale: owner created the class''s course or program';
                            ELSE
                                v_rule := 'legacy class sale: owner is neither the class''s instructor nor its creator';
                            END IF;
                        ELSIF t.description ~ '^Course sale earnings \(course [0-9a-fA-F-]{36}\)$' THEN
                            v_course := substring(t.description FROM '([0-9a-fA-F-]{36})')::UUID;
                            IF EXISTS (SELECT 1
                                       FROM courses c
                                                JOIN course_creators cc ON cc.uuid = c.course_creator_uuid
                                       WHERE c.uuid = v_course AND cc.user_uuid = w.user_uuid) THEN
                                v_purse := 'COURSE_CREATOR';
                                v_rule := 'legacy course sale: owner created the course';
                            ELSE
                                v_rule := 'legacy course sale: owner did not create the course';
                            END IF;
                        ELSE
                            v_rule := 'sale: no leg suffix and no legacy description';
                        END IF;
                    ELSIF t.transaction_type = 'PLATFORM_FEE' THEN
                        IF t.reference LIKE '%:creator:fee' THEN
                            v_purse := 'COURSE_CREATOR';
                            v_rule := 'fee: creator leg (:creator:fee)';
                        ELSIF t.reference LIKE '%:delivery:fee' THEN
                            v_purse := 'INSTRUCTOR';
                            v_rule := 'fee: delivery leg (:delivery:fee)';
                        ELSIF t.reference LIKE '%:fee' THEN
                            v_base := left(t.reference, -length(':fee'));
                            SELECT pp.purse INTO v_purse
                            FROM role_purse_plan pp
                                     JOIN user_wallet_transactions s ON s.id = pp.txn_id
                            WHERE s.wallet_id = w.id AND s.transaction_type = 'SALE' AND s.reference = v_base
                              AND pp.purse <> 'UNCLASSIFIED'
                            ORDER BY s.id
                            LIMIT 1;
                            v_rule := CASE WHEN v_purse IS NULL THEN 'fee: no sale with its reference in this wallet'
                                           ELSE 'fee: same purse as its sale' END;
                        ELSE
                            v_rule := 'fee: reference names no sale';
                        END IF;
                    ELSIF t.transaction_type = 'SALE_REVERSAL' THEN
                        IF t.reference LIKE '%:reversal' THEN
                            v_base := left(t.reference, -length(':reversal'));
                            SELECT pp.purse INTO v_purse
                            FROM role_purse_plan pp
                                     JOIN user_wallet_transactions s ON s.id = pp.txn_id
                            WHERE s.wallet_id = w.id AND s.transaction_type = 'SALE' AND s.reference = v_base
                              AND pp.purse <> 'UNCLASSIFIED'
                            ORDER BY s.id
                            LIMIT 1;
                            v_rule := CASE WHEN v_purse IS NULL THEN 'sale reversal: no sale with its reference in this wallet'
                                           ELSE 'sale reversal: purse of the sale it reverses' END;
                        ELSE
                            v_rule := 'sale reversal: reference names no sale';
                        END IF;
                    ELSIF t.transaction_type = 'DEPOSIT' THEN
                        v_purse := 'STUDENT';
                        v_rule := 'deposit: learner credit';
                    ELSIF t.transaction_type = 'TRANSFER_IN' THEN
                        v_purse := 'STUDENT';
                        v_rule := 'transfer in: learner credit';
                    ELSIF t.transaction_type IN ('WITHDRAWAL', 'PAYOUT_FEE', 'TRANSFER_OUT') THEN
                        v_fifo := TRUE;
                        v_rule := lower(replace(t.transaction_type, '_', ' ')) || ': oldest outstanding credits first';
                    ELSIF t.transaction_type = 'WITHDRAWAL_REVERSAL' AND t.reference LIKE '%:reversal' THEN
                        v_base := left(t.reference, -length(':reversal'));
                        v_back_sum := 0;
                        FOR v_back IN SELECT pp.purse, sum(pp.amount) AS amount, min(s.id) AS first_id
                                      FROM role_purse_plan pp
                                               JOIN user_wallet_transactions s ON s.id = pp.txn_id
                                      WHERE s.wallet_id = w.id
                                        AND ((s.transaction_type = 'WITHDRAWAL' AND s.reference = v_base)
                                          OR (s.transaction_type = 'PAYOUT_FEE' AND s.reference = v_base || ':fee'))
                                      GROUP BY pp.purse
                                      ORDER BY min(s.id), min(pp.portion)
                            LOOP
                                p_purse := array_append(p_purse, v_back.purse::TEXT);
                                p_amount := array_append(p_amount, v_back.amount);
                                v_back_sum := v_back_sum + v_back.amount;
                            END LOOP;
                        IF v_back_sum = t.amount THEN
                            v_rule := 'withdrawal reversal: back to the purses its withdrawal came from';
                        ELSE
                            p_purse := ARRAY []::TEXT[];
                            p_amount := ARRAY []::NUMERIC[];
                            v_rule := 'withdrawal reversal: amount does not match its withdrawal and charge';
                        END IF;
                    ELSE
                        v_rule := 'unrecognised row';
                    END IF;

                    IF v_fifo THEN
                        -- Take the oldest lots first, merging what comes from the same purse into one portion.
                        v_left := t.amount;
                        i := 1;
                        WHILE v_left > 0 AND i <= COALESCE(array_length(lot_amount, 1), 0)
                            LOOP
                                IF lot_amount[i] > 0 THEN
                                    v_take := least(lot_amount[i], v_left);
                                    lot_amount[i] := lot_amount[i] - v_take;
                                    v_left := v_left - v_take;
                                    j := array_position(p_purse, lot_purse[i]);
                                    IF j IS NULL THEN
                                        p_purse := array_append(p_purse, lot_purse[i]);
                                        p_amount := array_append(p_amount, v_take);
                                    ELSE
                                        p_amount[j] := p_amount[j] + v_take;
                                    END IF;
                                END IF;
                                i := i + 1;
                            END LOOP;
                        IF v_left > 0 THEN
                            p_purse := array_append(p_purse, 'UNCLASSIFIED'::TEXT);
                            p_amount := array_append(p_amount, v_left);
                            v_rule := v_rule || '; ' || v_left || ' not covered by classified credits';
                        END IF;
                    ELSIF COALESCE(array_length(p_purse, 1), 0) = 0 THEN
                        p_purse := ARRAY [COALESCE(v_purse, 'UNCLASSIFIED')];
                        p_amount := ARRAY [t.amount];
                    END IF;

                    FOR k IN 1 .. array_length(p_purse, 1)
                        LOOP
                            IF p_purse[k] = 'UNCLASSIFIED' THEN
                                problems := array_append(problems, format('row %s (%s %s, reference %s) is unclassified: %s',
                                                               t.id, t.transaction_type, t.amount, t.reference, v_rule));
                            ELSIF NOT v_credit AND NOT v_fifo THEN
                                -- A debit that names its purse takes that purse's oldest lots.
                                v_left := p_amount[k];
                                i := 1;
                                WHILE v_left > 0 AND i <= COALESCE(array_length(lot_amount, 1), 0)
                                    LOOP
                                        IF lot_purse[i] = p_purse[k] AND lot_amount[i] > 0 THEN
                                            v_take := least(lot_amount[i], v_left);
                                            lot_amount[i] := lot_amount[i] - v_take;
                                            v_left := v_left - v_take;
                                        END IF;
                                        i := i + 1;
                                    END LOOP;
                                IF v_left > 0 THEN
                                    problems := array_append(problems, format('row %s (%s %s) takes %s more than the %s purse holds',
                                                                   t.id, t.transaction_type, t.amount, v_left, p_purse[k]));
                                END IF;
                            ELSIF v_credit AND t.transaction_type = 'WITHDRAWAL_REVERSAL' THEN
                                -- Money coming back was the oldest out, so it is the oldest again, in its old order.
                                lot_purse := array_cat(array_append(lot_purse[1:k - 1], p_purse[k]), lot_purse[k:]);
                                lot_amount := array_cat(array_append(lot_amount[1:k - 1], p_amount[k]), lot_amount[k:]);
                            ELSIF v_credit THEN
                                lot_purse := array_append(lot_purse, p_purse[k]);
                                lot_amount := array_append(lot_amount, p_amount[k]);
                            END IF;
                            bal := jsonb_set(bal, ARRAY [p_purse[k]], to_jsonb((bal ->> p_purse[k])::NUMERIC
                                + CASE WHEN v_credit THEN p_amount[k] ELSE -p_amount[k] END));
                            INSERT INTO role_purse_plan (txn_id, wallet_id, portion, purse, amount, rule)
                            VALUES (t.id, w.id, k, p_purse[k], p_amount[k], v_rule);
                            IF v_first IS NULL AND p_purse[k] <> 'UNCLASSIFIED' THEN
                                v_first := p_purse[k];
                            END IF;
                        END LOOP;
                END LOOP;

            IF v_first IS NULL THEN
                -- No history: the row goes to the purse of the owner's role.
                v_first := CASE
                               WHEN EXISTS (SELECT 1 FROM instructors x WHERE x.user_uuid = w.user_uuid) THEN 'INSTRUCTOR'
                               WHEN EXISTS (SELECT 1 FROM course_creators x WHERE x.user_uuid = w.user_uuid) THEN 'COURSE_CREATOR'
                               ELSE 'STUDENT' END;
            END IF;

            SELECT sum(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END) INTO v_ledger
            FROM ledger_accounts la
                     JOIN ledger_entries e ON e.account_uuid = la.uuid
            WHERE la.owner_type = 'USER' AND la.owner_uuid = w.user_uuid AND la.purse = 'EARNINGS'
              AND la.currency_code = w.currency_code;
            v_ledger := COALESCE(v_ledger, 0);

            IF (bal ->> 'COURSE_CREATOR')::NUMERIC < 0 OR (bal ->> 'INSTRUCTOR')::NUMERIC < 0
                OR (bal ->> 'STUDENT')::NUMERIC < 0 THEN
                problems := array_append(problems, format('a purse ends below zero: %s', bal));
            END IF;
            IF (bal ->> 'COURSE_CREATOR')::NUMERIC + (bal ->> 'INSTRUCTOR')::NUMERIC + (bal ->> 'STUDENT')::NUMERIC
                   + (bal ->> 'UNCLASSIFIED')::NUMERIC <> w.balance_amount THEN
                problems := array_append(problems, format('the purses sum to %s but the wallet holds %s',
                                               (bal ->> 'COURSE_CREATOR')::NUMERIC + (bal ->> 'INSTRUCTOR')::NUMERIC
                                                   + (bal ->> 'STUDENT')::NUMERIC + (bal ->> 'UNCLASSIFIED')::NUMERIC,
                                               w.balance_amount));
            END IF;
            IF v_ledger <> w.balance_amount THEN
                problems := array_append(problems, format('the ledger EARNINGS account holds %s but the wallet holds %s',
                                               v_ledger, w.balance_amount));
            END IF;

            INSERT INTO role_purse_wallets (wallet_id, owner_uuid, currency_code, balance_amount, course_creator,
                                            instructor, student, unclassified, ledger_balance, first_purse, problems)
            VALUES (w.id, w.user_uuid, w.currency_code, w.balance_amount, (bal ->> 'COURSE_CREATOR')::NUMERIC,
                    (bal ->> 'INSTRUCTOR')::NUMERIC, (bal ->> 'STUDENT')::NUMERIC, (bal ->> 'UNCLASSIFIED')::NUMERIC,
                    v_ledger, v_first, problems);
        END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.plan_role_purses();
-- @@role-purse-plan:end

-- Refuse rather than guess.
DO
$$
    DECLARE
        v_count  INT;
        v_detail TEXT;
    BEGIN
        SELECT count(*) INTO v_count FROM role_purse_wallets WHERE cardinality(problems) > 0;
        IF v_count > 0 THEN
            SELECT string_agg(format('wallet %s (user %s, %s): %s', wallet_id, owner_uuid, currency_code,
                                     array_to_string(problems, '; ')), E'\n')
            INTO v_detail
            FROM (SELECT * FROM role_purse_wallets WHERE cardinality(problems) > 0 ORDER BY wallet_id LIMIT 20) p;
            RAISE EXCEPTION E'Role purse backfill refused: % wallet(s) cannot be split exactly; run scripts/role-purse-backfill-preview.sql for the full list.\n%',
                v_count, v_detail USING ERRCODE = '23514';
        END IF;
    END
$$;

DO
$$
    DECLARE
        rw     RECORD;
        v_txn  UUID;
    BEGIN
        FOR rw IN SELECT pw.*, uw.uuid AS wallet_uuid
                  FROM role_purse_wallets pw
                           JOIN user_wallets uw ON uw.id = pw.wallet_id
                  ORDER BY pw.wallet_id
            LOOP
                -- The existing row keeps its uuid and becomes the purse of its oldest row; the others are new rows.
                UPDATE user_wallets SET purse = rw.first_purse, updated_date = CURRENT_TIMESTAMP WHERE id = rw.wallet_id;
                INSERT INTO user_wallets (owner_type, user_uuid, currency_code, balance_amount, purse, created_date,
                                          created_by)
                SELECT 'USER', rw.owner_uuid, rw.currency_code, 0, p.purse, min(t.created_date), 'SYSTEM'
                FROM role_purse_plan p
                         JOIN user_wallet_transactions t ON t.id = p.txn_id
                WHERE p.wallet_id = rw.wallet_id
                  AND p.purse <> rw.first_purse
                GROUP BY p.purse;

                -- Each row moves to its purse; a debit spread over two purses keeps its first portion and gains a copy
                -- for the rest, at the same moment and with the same reference.
                UPDATE user_wallet_transactions t
                SET wallet_id = target.id, amount = p.amount, updated_date = CURRENT_TIMESTAMP
                FROM role_purse_plan p
                         JOIN user_wallets target ON target.owner_type = 'USER' AND target.user_uuid = rw.owner_uuid
                    AND target.currency_code = rw.currency_code AND target.purse = p.purse
                WHERE p.wallet_id = rw.wallet_id
                  AND p.portion = 1
                  AND t.id = p.txn_id;
                INSERT INTO user_wallet_transactions (wallet_id, transaction_type, amount, currency_code,
                                                      balance_before, balance_after, reference, description,
                                                      transfer_reference, counterparty_user_uuid, created_date,
                                                      created_by)
                SELECT target.id, t.transaction_type, p.amount, t.currency_code, 0, 0, t.reference, t.description,
                       t.transfer_reference, t.counterparty_user_uuid, t.created_date, t.created_by
                FROM role_purse_plan p
                         JOIN user_wallet_transactions t ON t.id = p.txn_id
                         JOIN user_wallets target ON target.owner_type = 'USER' AND target.user_uuid = rw.owner_uuid
                    AND target.currency_code = rw.currency_code AND target.purse = p.purse
                WHERE p.wallet_id = rw.wallet_id
                  AND p.portion > 1
                ORDER BY t.id, p.portion;

                -- balance_before and balance_after are now the purse's own running balance.
                WITH chain AS (
                    SELECT t.id,
                           CASE WHEN t.transaction_type IN ('SALE', 'DEPOSIT', 'TRANSFER_IN', 'WITHDRAWAL_REVERSAL')
                                    THEN t.amount ELSE -t.amount END AS signed,
                           sum(CASE WHEN t.transaction_type IN ('SALE', 'DEPOSIT', 'TRANSFER_IN', 'WITHDRAWAL_REVERSAL')
                                        THEN t.amount ELSE -t.amount END)
                           OVER (PARTITION BY t.wallet_id ORDER BY t.created_date, t.id) AS running
                    FROM user_wallet_transactions t
                             JOIN user_wallets uw ON uw.id = t.wallet_id
                    WHERE uw.owner_type = 'USER' AND uw.user_uuid = rw.owner_uuid
                      AND uw.currency_code = rw.currency_code)
                UPDATE user_wallet_transactions t
                SET balance_after = c.running, balance_before = c.running - c.signed
                FROM chain c
                WHERE t.id = c.id;

                UPDATE user_wallets uw
                SET balance_amount = CASE uw.purse
                                         WHEN 'COURSE_CREATOR' THEN rw.course_creator
                                         WHEN 'INSTRUCTOR' THEN rw.instructor
                                         WHEN 'STUDENT' THEN rw.student END,
                    updated_date   = CURRENT_TIMESTAMP
                WHERE uw.owner_type = 'USER' AND uw.user_uuid = rw.owner_uuid AND uw.currency_code = rw.currency_code;

                IF EXISTS (SELECT 1
                           FROM user_wallets uw
                           WHERE uw.owner_type = 'USER' AND uw.user_uuid = rw.owner_uuid
                             AND uw.currency_code = rw.currency_code
                             AND uw.balance_amount <> COALESCE((SELECT t.balance_after
                                                                FROM user_wallet_transactions t
                                                                WHERE t.wallet_id = uw.id
                                                                ORDER BY t.created_date DESC, t.id DESC
                                                                LIMIT 1), 0)) THEN
                    RAISE EXCEPTION 'Wallet % split into purses whose rows do not end at their balances', rw.wallet_uuid
                        USING ERRCODE = '23514';
                END IF;

                -- One ledger account per purse row, and one balanced posting moving the EARNINGS balance into them.
                INSERT INTO ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, status,
                                             created_by)
                SELECT 'USER', uw.user_uuid, 'LIABILITY', uw.purse, uw.currency_code, 'ACTIVE', 'SYSTEM'
                FROM user_wallets uw
                WHERE uw.owner_type = 'USER' AND uw.user_uuid = rw.owner_uuid AND uw.currency_code = rw.currency_code
                ON CONFLICT DO NOTHING;

                IF rw.balance_amount <> 0
                    AND NOT EXISTS (SELECT 1 FROM ledger_transactions
                                    WHERE idempotency_key = 'wallet-purse-split:' || rw.wallet_uuid) THEN
                    INSERT INTO ledger_transactions (idempotency_key, occurred_at, description, cause_type, cause_uuid,
                                                     created_by)
                    VALUES ('wallet-purse-split:' || rw.wallet_uuid, CURRENT_TIMESTAMP,
                            'Wallet balance split into role purses', 'WALLET_PURSE_SPLIT', rw.wallet_uuid, 'SYSTEM')
                    RETURNING uuid INTO v_txn;
                    INSERT INTO ledger_entries (transaction_uuid, account_uuid, direction, amount, currency_code,
                                                created_by)
                    SELECT v_txn, la.uuid, leg.direction, leg.amount, rw.currency_code, 'SYSTEM'
                    FROM (VALUES ('EARNINGS', 'DEBIT', rw.balance_amount),
                                 ('COURSE_CREATOR', 'CREDIT', rw.course_creator),
                                 ('INSTRUCTOR', 'CREDIT', rw.instructor),
                                 ('STUDENT', 'CREDIT', rw.student)) AS leg(purse, direction, amount)
                             JOIN ledger_accounts la ON la.owner_type = 'USER' AND la.owner_uuid = rw.owner_uuid
                        AND la.purse = leg.purse AND la.currency_code = rw.currency_code
                    WHERE leg.amount > 0;
                END IF;

                INSERT INTO wallet_audit_events (owner_type, owner_uuid, currency_code, event_type, reference, reason,
                                                 previous_values, updated_values, created_by)
                VALUES ('USER', rw.owner_uuid, rw.currency_code, 'WALLET_SPLIT_INTO_PURSES', rw.wallet_uuid::TEXT,
                        'Wallet split into role purses from its own history',
                        jsonb_build_object('balance_amount', rw.balance_amount),
                        jsonb_build_object('COURSE_CREATOR', rw.course_creator, 'INSTRUCTOR', rw.instructor,
                                           'STUDENT', rw.student, 'rows', (SELECT count(*) FROM role_purse_plan p
                                                                          WHERE p.wallet_id = rw.wallet_id)),
                        'SYSTEM');
            END LOOP;

        -- A withdrawal is from the purse its debit came from (the larger part, should FIFO have spread it); one that
        -- never debited anything is from the owner's fullest purse.
        UPDATE withdrawals wd
        SET purse = x.purse
        FROM (SELECT DISTINCT ON (wd2.id) wd2.id, uw.purse
              FROM withdrawals wd2
                       JOIN user_wallets uw ON uw.owner_type = 'USER' AND uw.user_uuid = wd2.owner_uuid
                  AND uw.currency_code = wd2.currency_code
                       JOIN user_wallet_transactions t ON t.wallet_id = uw.id AND t.transaction_type = 'WITHDRAWAL'
                  AND t.reference = wd2.reference
              WHERE wd2.owner_type = 'USER' AND wd2.purse IS NULL
              GROUP BY wd2.id, uw.purse
              ORDER BY wd2.id, sum(t.amount) DESC, min(t.id)) x
        WHERE wd.id = x.id;
        UPDATE withdrawals wd
        SET purse = COALESCE(
                (SELECT uw.purse
                 FROM user_wallets uw
                 WHERE uw.owner_type = 'USER' AND uw.user_uuid = wd.owner_uuid AND uw.currency_code = wd.currency_code
                 ORDER BY uw.balance_amount DESC,
                          array_position(ARRAY ['COURSE_CREATOR', 'INSTRUCTOR', 'STUDENT']::VARCHAR[], uw.purse)
                 LIMIT 1),
                CASE WHEN EXISTS (SELECT 1 FROM instructors x WHERE x.user_uuid = wd.owner_uuid) THEN 'INSTRUCTOR'
                     WHEN EXISTS (SELECT 1 FROM course_creators x WHERE x.user_uuid = wd.owner_uuid) THEN 'COURSE_CREATOR'
                     ELSE 'STUDENT' END)
        WHERE wd.owner_type = 'USER' AND wd.purse IS NULL;

        -- Cached balances rebuilt from the entries, as every earlier ledger backfill does.
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

ALTER TABLE user_wallets ALTER COLUMN purse SET NOT NULL;
ALTER TABLE user_wallets DROP CONSTRAINT IF EXISTS chk_user_wallet_purse;
ALTER TABLE user_wallets ADD CONSTRAINT chk_user_wallet_purse CHECK (
    (owner_type = 'USER' AND purse IN ('COURSE_CREATOR', 'INSTRUCTOR', 'STUDENT'))
        OR (owner_type = 'ORGANISATION' AND purse = 'EARNINGS'));
ALTER TABLE user_wallets DROP CONSTRAINT IF EXISTS uq_user_wallet_owner_purse_currency;
ALTER TABLE user_wallets ADD CONSTRAINT uq_user_wallet_owner_purse_currency
    UNIQUE (owner_type, user_uuid, purse, currency_code);
COMMENT ON COLUMN user_wallets.purse IS
    'COURSE_CREATOR, INSTRUCTOR or STUDENT for a user; EARNINGS for an organisation. One row per owner, purse and currency.';

ALTER TABLE withdrawals ALTER COLUMN purse SET NOT NULL;
ALTER TABLE withdrawals DROP CONSTRAINT IF EXISTS chk_withdrawal_purse;
ALTER TABLE withdrawals ADD CONSTRAINT chk_withdrawal_purse CHECK (
    (owner_type = 'USER' AND purse IN ('COURSE_CREATOR', 'INSTRUCTOR', 'STUDENT'))
        OR (owner_type = 'ORGANISATION' AND purse = 'EARNINGS'));
