-- Read-only preview of the role purse backfill (V202611042152__split_wallets_into_role_purses.sql).
--
-- Usage, against staging before deploying:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f scripts/role-purse-backfill-preview.sql > role-purse-preview.txt
--
-- It runs the migration's own plan (the block between the @@role-purse-plan markers is identical in both files;
-- a test keeps them so) inside a transaction that is rolled back. The plan writes only two temporary tables; no
-- table of the application is changed. Every wallet listed under "Problems" would make the migration refuse to run.
\set ON_ERROR_STOP on
\pset pager off
\pset null '-'

BEGIN;

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

\echo
\echo '== Per wallet: current balance and the purses it splits into'
SELECT w.wallet_id,
       w.owner_uuid,
       NULLIF(trim(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')), '') AS owner,
       u.email,
       w.currency_code,
       w.balance_amount                                                                 AS current_balance,
       w.course_creator,
       w.instructor,
       w.student,
       w.unclassified,
       w.ledger_balance                                                                 AS ledger_earnings,
       w.first_purse                                                                    AS existing_row_becomes,
       CASE WHEN cardinality(w.problems) = 0 THEN 'ok' ELSE 'BLOCKS THE MIGRATION' END  AS status
FROM role_purse_wallets w
         LEFT JOIN users u ON u.uuid = w.owner_uuid
ORDER BY w.balance_amount DESC, w.wallet_id;

\echo
\echo '== Totals'
SELECT count(*)                                              AS wallets,
       sum(balance_amount)                                   AS current_balance,
       sum(course_creator)                                   AS course_creator,
       sum(instructor)                                       AS instructor,
       sum(student)                                          AS student,
       sum(unclassified)                                     AS unclassified,
       count(*) FILTER (WHERE cardinality(problems) > 0)     AS wallets_blocking
FROM role_purse_wallets;

\echo
\echo '== Rules used'
SELECT rule, purse, count(*) AS rows, sum(amount) AS amount
FROM role_purse_plan
GROUP BY rule, purse
ORDER BY rule, purse;

\echo
\echo '== Every row not classified by a :creator or :delivery suffix, with the rule that classified it'
SELECT p.wallet_id,
       t.id      AS txn_id,
       t.created_date,
       t.transaction_type,
       t.amount  AS row_amount,
       p.portion,
       p.purse,
       p.amount  AS portion_amount,
       t.reference,
       t.description,
       p.rule
FROM role_purse_plan p
         JOIN user_wallet_transactions t ON t.id = p.txn_id
WHERE p.rule NOT LIKE '%(:creator%'
  AND p.rule NOT LIKE '%(:delivery%'
ORDER BY p.wallet_id, t.created_date, t.id, p.portion;

\echo
\echo '== Debits spread over more than one purse (each extra portion becomes a row of its own)'
SELECT p.wallet_id, t.id AS txn_id, t.transaction_type, t.reference, t.amount,
       string_agg(p.purse || ' ' || p.amount, ', ' ORDER BY p.portion) AS portions
FROM role_purse_plan p
         JOIN user_wallet_transactions t ON t.id = p.txn_id
GROUP BY p.wallet_id, t.id, t.transaction_type, t.reference, t.amount
HAVING count(*) > 1
ORDER BY p.wallet_id, t.id;

\echo
\echo '== Withdrawals of people and the purse each will belong to'
SELECT wd.reference,
       wd.status,
       wd.amount,
       wd.owner_uuid,
       COALESCE(string_agg(p.purse || ' ' || p.amount, ', ' ORDER BY p.portion),
                'no debit yet: the owner''s fullest purse') AS debited_from
FROM withdrawals wd
         LEFT JOIN user_wallets uw ON uw.owner_type = 'USER' AND uw.user_uuid = wd.owner_uuid
    AND uw.currency_code = wd.currency_code
         LEFT JOIN user_wallet_transactions t ON t.wallet_id = uw.id AND t.transaction_type = 'WITHDRAWAL'
    AND t.reference = wd.reference
         LEFT JOIN role_purse_plan p ON p.txn_id = t.id
WHERE wd.owner_type = 'USER'
GROUP BY wd.id, wd.reference, wd.status, wd.amount, wd.owner_uuid
ORDER BY wd.id;

\echo
\echo '== Problems (any row here makes the migration refuse to run)'
SELECT wallet_id, owner_uuid, currency_code, unnest(problems) AS problem
FROM role_purse_wallets
WHERE cardinality(problems) > 0
ORDER BY wallet_id;

ROLLBACK;
