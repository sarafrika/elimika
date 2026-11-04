-- Legacy wallet history for RolePurseBackfillMigrationTest, written as the pre-purse application left it.
-- People: A teaches K1 (course X by Z) and created course Y (class K2, taught by D). F only holds learner credit.
INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) VALUES
    ('a0000000-0000-0000-0000-00000000000a', 'Ann', 'Both', 'ann@test.local', '900000001', 'seed'),
    ('a0000000-0000-0000-0000-00000000000d', 'Dan', 'Teach', 'dan@test.local', '900000002', 'seed'),
    ('a0000000-0000-0000-0000-00000000000f', 'Fay', 'Learn', 'fay@test.local', '900000003', 'seed'),
    ('a0000000-0000-0000-0000-000000000002', 'Zed', 'Make', 'zed@test.local', '900000004', 'seed');
INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES
    ('c0000000-0000-0000-0000-00000000000a', 'a0000000-0000-0000-0000-00000000000a', 'Ann', 'seed'),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'Zed', 'seed');
INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) VALUES
    ('10000000-0000-0000-0000-00000000000a', 'a0000000-0000-0000-0000-00000000000a', 'Ann', true, 'seed'),
    ('10000000-0000-0000-0000-00000000000d', 'a0000000-0000-0000-0000-00000000000d', 'Dan', true, 'seed');
INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) VALUES
    ('e0000000-0000-0000-0000-00000000000a', 'Course X', 'c0000000-0000-0000-0000-000000000002', 'published', true, 'seed'),
    ('e0000000-0000-0000-0000-00000000000b', 'Course Y', 'c0000000-0000-0000-0000-00000000000a', 'published', true, 'seed');
INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, default_start_time, default_end_time,
                               location_type, class_visibility, session_format, max_participants, is_active,
                               registration_period_start_date, registration_period_end_date, created_by) VALUES
    ('d0000000-0000-0000-0000-0000000000b1', 'K1', '10000000-0000-0000-0000-00000000000a', 'e0000000-0000-0000-0000-00000000000a',
     TIMESTAMPTZ '2026-05-05 09:00:00+00', TIMESTAMPTZ '2026-05-05 09:45:00+00', 'IN_PERSON', 'PUBLIC', 'GROUP', 20, true,
     DATE '2026-01-01', DATE '2027-01-01', 'seed'),
    ('d0000000-0000-0000-0000-0000000000b2', 'K2', '10000000-0000-0000-0000-00000000000d', 'e0000000-0000-0000-0000-00000000000b',
     TIMESTAMPTZ '2026-05-05 09:00:00+00', TIMESTAMPTZ '2026-05-05 09:45:00+00', 'IN_PERSON', 'PUBLIC', 'GROUP', 20, true,
     DATE '2026-01-01', DATE '2027-01-01', 'seed');

-- Writes a wallet row the way the application did before purses: running balance on the one wallet.
CREATE FUNCTION pg_temp.seed_txn(p_owner_type TEXT, p_owner UUID, p_type TEXT, p_amount NUMERIC, p_ref TEXT,
                                 p_description TEXT, p_at TIMESTAMP) RETURNS VOID AS
$$
DECLARE
    v_wallet user_wallets%ROWTYPE;
    v_delta  NUMERIC := CASE WHEN p_type IN ('SALE', 'DEPOSIT', 'TRANSFER_IN', 'WITHDRAWAL_REVERSAL') THEN p_amount
                             ELSE -p_amount END;
BEGIN
    INSERT INTO user_wallets (owner_type, user_uuid, currency_code, balance_amount, created_date, created_by)
    VALUES (p_owner_type, p_owner, 'KES', 0, p_at, 'seed')
    ON CONFLICT DO NOTHING;
    SELECT * INTO v_wallet FROM user_wallets WHERE owner_type = p_owner_type AND user_uuid = p_owner AND currency_code = 'KES';
    INSERT INTO user_wallet_transactions (wallet_id, transaction_type, amount, currency_code, balance_before,
                                          balance_after, reference, description, created_date, created_by)
    VALUES (v_wallet.id, p_type, p_amount, 'KES', v_wallet.balance_amount, v_wallet.balance_amount + v_delta, p_ref,
            p_description, p_at, 'seed');
    UPDATE user_wallets SET balance_amount = balance_amount + v_delta WHERE id = v_wallet.id;
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'SALE', 1000, 'o1:l1',
                        'Class sale earnings (class d0000000-0000-0000-0000-0000000000b1)', TIMESTAMP '2026-08-01 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'SALE', 300, 'o4:l1',
                        'Class sale earnings (class d0000000-0000-0000-0000-0000000000b2)', TIMESTAMP '2026-08-02 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'SALE_REVERSAL', 300, 'o4:l1:reversal',
                        'Moved to the hiring organisation - K2', TIMESTAMP '2026-08-03 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'SALE', 600, 'o2:l1:creator',
                        'Class sale share - K2', TIMESTAMP '2026-10-21 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'PLATFORM_FEE', 60, 'o2:l1:creator:fee',
                        'Platform fee - Class sale share - K2', TIMESTAMP '2026-10-21 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'SALE', 400, 'o3:l1:delivery',
                        'Class sale share - K1', TIMESTAMP '2026-10-22 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'PLATFORM_FEE', 40, 'o3:l1:delivery:fee',
                        'Platform fee - Class sale share - K1', TIMESTAMP '2026-10-22 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'WITHDRAWAL', 1200, 'WD-2026-000001',
                        'Withdrawal to M-Pesa', TIMESTAMP '2026-10-23 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'WITHDRAWAL_REVERSAL', 1200,
                        'WD-2026-000001:reversal', 'Withdrawal returned', TIMESTAMP '2026-10-24 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000a', 'WITHDRAWAL', 500, 'WD-2026-000002',
                        'Withdrawal to M-Pesa', TIMESTAMP '2026-10-25 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000f', 'DEPOSIT', 100, 'topup-1', 'Top up',
                        TIMESTAMP '2026-09-01 10:00');
SELECT pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-00000000000f', 'TRANSFER_OUT', 30, 'gift-1', 'Gift',
                        TIMESTAMP '2026-09-02 10:00');
SELECT pg_temp.seed_txn('ORGANISATION', 'b0000000-0000-0000-0000-00000000000b', 'SALE', 500, 'o5:l1:delivery',
                        'Class sale share - K3', TIMESTAMP '2026-10-22 11:00');

INSERT INTO withdrawals (reference, owner_type, owner_uuid, currency_code, amount, fee_amount, net_amount,
                         payout_method_uuid, msisdn, status, idempotency_key, created_by, created_date) VALUES
    ('WD-2026-000001', 'USER', 'a0000000-0000-0000-0000-00000000000a', 'KES', 1200, 0, 1200,
     '90000000-0000-0000-0000-000000000001', '254700000001', 'FAILED', 'k1', 'seed', TIMESTAMP '2026-10-23 09:59'),
    ('WD-2026-000002', 'USER', 'a0000000-0000-0000-0000-00000000000a', 'KES', 500, 0, 500,
     '90000000-0000-0000-0000-000000000001', '254700000001', 'NEEDS_REVIEW', 'k2', 'seed', TIMESTAMP '2026-10-25 09:59'),
    ('WD-2026-000003', 'USER', 'a0000000-0000-0000-0000-00000000000f', 'KES', 50, 0, 50,
     '90000000-0000-0000-0000-000000000002', '254700000002', 'CANCELLED', 'k3', 'seed', TIMESTAMP '2026-10-25 09:59');

-- The ledger as the dual write left it: each wallet's EARNINGS account holds its balance.
INSERT INTO ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, created_by)
SELECT w.owner_type, w.user_uuid, 'LIABILITY', 'EARNINGS', 'KES', 'seed' FROM user_wallets w
ON CONFLICT DO NOTHING;
INSERT INTO ledger_transactions (idempotency_key, description, created_by)
SELECT 'seed:' || w.uuid, 'seed', 'seed' FROM user_wallets w WHERE w.balance_amount > 0;
INSERT INTO ledger_entries (transaction_uuid, account_uuid, direction, amount, currency_code, created_by)
SELECT t.uuid, leg.account, leg.direction, w.balance_amount, 'KES', 'seed'
FROM user_wallets w
         JOIN ledger_transactions t ON t.idempotency_key = 'seed:' || w.uuid
         JOIN ledger_accounts ua ON ua.owner_type = w.owner_type AND ua.owner_uuid = w.user_uuid AND ua.purse = 'EARNINGS'
         JOIN ledger_accounts pa ON pa.owner_type = 'PLATFORM' AND pa.purse = 'PLATFORM_UNALLOCATED_REVENUE'
    AND pa.currency_code = 'KES'
         CROSS JOIN LATERAL (VALUES (ua.uuid, 'CREDIT'), (pa.uuid, 'DEBIT')) AS leg(account, direction);
