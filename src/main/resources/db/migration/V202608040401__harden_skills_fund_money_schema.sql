-- Make the skills fund capable of holding real money.
--
-- The fund was built as a reporting construct: amounts with no currency, a status column that was
-- whatever string the caller typed, a recipient recorded only as a display name, and funding sources
-- that could be hard-deleted out from under the arithmetic that depended on them. None of that is
-- survivable once a disbursement is expected to actually move money. This migration fixes the four
-- correctness problems and nothing else — no wallet linkage, no events.
--
-- ---------------------------------------------------------------------------------------------
-- 1. CURRENCY. Every amount here was a bare number and `getSummary` added them together as if they
--    were commensurable. Both tables now carry `currency_code` NOT NULL with a foreign key to
--    `currencies`, matching `user_wallets`, `commerce_purchase` and `instructor_obligations`.
--
--    BACKFILL ASSUMPTION: existing rows are declared to be KES. This is an assertion, not a
--    fabrication. The `currencies` seed leaves KES as the platform's single active and default
--    currency and deactivates every other code; the organisation skills fund UI hard-codes the
--    "KSh" prefix on every figure it renders and labels both amount inputs "Amount (KSh)"; and the
--    precedent migration V202510301918 backfilled `commerce_catalog_item.currency_code` and
--    `course_training_applications.rate_currency` to KES on exactly the same reasoning. There is no
--    other currency a pre-existing row could have been denominated in, because no other currency
--    could be selected anywhere in the product. Should that ever stop being true, this comment is
--    the record of what was assumed and when.
--
--    Deliberately NO column default. A default would let a future insert that forgot to state a
--    currency silently become KES, which is the fabrication we are avoiding for new data even
--    though it is a safe reading of old data. New rows must say what they are denominated in.
--
-- 2. STATUS. Was VARCHAR(50) with no constraint, compared case-insensitively in Java, so any typo
--    was accepted on write and then fell out of every aggregate — the money did not vanish from the
--    table, it vanished from the totals. Now an explicit four-value enum with a CHECK constraint.
--
--    The legacy value set was read out of the code, not guessed: SkillsFundServiceImpl.getSummary
--    filters on Completed/Disbursed/Allocated/Approved/Pending, the column default was 'Pending',
--    and the organisation dashboard offers exactly Pending/Allocated/Approved/Completed in its
--    status select. Mapping (case- and whitespace-insensitive):
--
--        Pending   -> PENDING
--        Allocated -> ALLOCATED
--        Approved  -> APPROVED
--        Completed -> DISBURSED
--        Disbursed -> DISBURSED
--
--    'Completed' and 'Disbursed' collapse because they named the same real-world fact: the money
--    left the fund. The UI could only ever produce 'Completed'; 'Disbursed' existed solely in the
--    backend filter. Keeping both would preserve a distinction the product never made.
--
--    Anything that maps to nothing ABORTS this migration. A status the code never referenced is a
--    row whose money is already excluded from every total, and quietly rewriting it to PENDING or
--    dropping it would turn a visible data problem into an invisible one. The DO block below names
--    the offending values in the error so they can be corrected and the migration re-run.
--
-- 3. BENEFICIARY. `target_name` was a display string; a disbursement that cannot name its recipient
--    can never become money. `beneficiary_user_uuid` is added alongside it — `target_name` is kept
--    because the dashboard renders it in the "Target" column and sends it on create, and because it
--    remains the only readable trace when a beneficiary account is later removed.
--
--    NO BACKFILL, and the column is nullable. Existing rows hold a free-typed name such as
--    "John Doe" with no organisation scoping, no uniqueness and no guarantee the person is even a
--    platform user. Resolving those to a uuid would mean guessing which account a string meant, and
--    a wrong guess here eventually credits the wrong person's wallet. An unidentifiable historical
--    row must stay unidentified.
--
-- 4. SOURCE DELETION. Sources are now soft-deleted, following the `deleted BOOLEAN NOT NULL DEFAULT
--    FALSE` convention already used by `organisation`, `training_branches` and
--    `user_organisation_domain_mapping`. A funding source is an input to a published balance; hard
--    deleting one silently rewrites history. The service additionally refuses the removal when the
--    surviving sources could no longer cover what has already been disbursed.

-- ---------------------------------------------------------------------------------------------
-- skills_fund_sources
-- ---------------------------------------------------------------------------------------------

ALTER TABLE skills_fund_sources
    ADD COLUMN currency_code VARCHAR(3);

-- See the BACKFILL ASSUMPTION note above: KES is asserted, on the grounds that no other currency
-- was reachable in the product at the time these rows were written.
UPDATE skills_fund_sources
SET currency_code = 'KES'
WHERE currency_code IS NULL;

ALTER TABLE skills_fund_sources
    ALTER COLUMN currency_code SET NOT NULL;

ALTER TABLE skills_fund_sources
    ADD CONSTRAINT fk_skills_fund_sources_currency
        FOREIGN KEY (currency_code) REFERENCES currencies (code),
    ADD CONSTRAINT chk_skills_fund_sources_currency_length
        CHECK (char_length(currency_code) = 3);

ALTER TABLE skills_fund_sources
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Every read of a fund's sources is "the live ones for this organisation".
CREATE INDEX idx_skills_fund_sources_organisation_live
    ON skills_fund_sources (organisation_uuid) WHERE NOT deleted;

COMMENT ON COLUMN skills_fund_sources.currency_code IS
    'ISO-4217 currency this source is denominated in. Pre-existing rows were backfilled to KES, the platform''s only active currency at the time they were written.';
COMMENT ON COLUMN skills_fund_sources.deleted IS
    'Soft-delete flag. A funding source is an input to a published balance, so removing one hides it rather than erasing it.';

-- ---------------------------------------------------------------------------------------------
-- skills_fund_transactions — currency
-- ---------------------------------------------------------------------------------------------

ALTER TABLE skills_fund_transactions
    ADD COLUMN currency_code VARCHAR(3);

UPDATE skills_fund_transactions
SET currency_code = 'KES'
WHERE currency_code IS NULL;

ALTER TABLE skills_fund_transactions
    ALTER COLUMN currency_code SET NOT NULL;

ALTER TABLE skills_fund_transactions
    ADD CONSTRAINT fk_skills_fund_transactions_currency
        FOREIGN KEY (currency_code) REFERENCES currencies (code),
    ADD CONSTRAINT chk_skills_fund_transactions_currency_length
        CHECK (char_length(currency_code) = 3);

COMMENT ON COLUMN skills_fund_transactions.currency_code IS
    'ISO-4217 currency this movement is denominated in. Pre-existing rows were backfilled to KES, the platform''s only active currency at the time they were written.';

-- ---------------------------------------------------------------------------------------------
-- skills_fund_transactions — beneficiary
-- ---------------------------------------------------------------------------------------------

-- ON DELETE SET NULL rather than RESTRICT or CASCADE: a fund movement is a financial record and must
-- outlive the account it named, but a removed account must not block its own removal. `target_name`
-- survives as the readable trace of who the row was for.
ALTER TABLE skills_fund_transactions
    ADD COLUMN beneficiary_user_uuid UUID
        REFERENCES users (uuid) ON DELETE SET NULL;

CREATE INDEX idx_skills_fund_transactions_beneficiary
    ON skills_fund_transactions (beneficiary_user_uuid);

COMMENT ON COLUMN skills_fund_transactions.beneficiary_user_uuid IS
    'The platform user this movement is for. Nullable and never backfilled: rows written before this column existed carry only a free-typed target_name, and guessing which account that string meant would eventually credit the wrong person.';
COMMENT ON COLUMN skills_fund_transactions.target_name IS
    'Display label for the recipient. Kept alongside beneficiary_user_uuid for rendering and as the surviving trace when a beneficiary account is removed. Not an identity — never resolve money against this.';

-- ---------------------------------------------------------------------------------------------
-- skills_fund_transactions — status enum
-- ---------------------------------------------------------------------------------------------

-- The default has to go before the column can be rewritten, otherwise 'Pending' fails the CHECK.
ALTER TABLE skills_fund_transactions
    ALTER COLUMN status DROP DEFAULT;

-- Fail loudly on anything the code never referenced, BEFORE any rewriting, so the error names the
-- value exactly as it is stored and an operator can go and find the row. Silently coercing an unknown
-- status would take a row whose money is already missing from every total and make the fact
-- undiscoverable.
DO
$$
    DECLARE
        stray TEXT;
    BEGIN
        SELECT string_agg(DISTINCT COALESCE(status, '<null>'), ', ')
        INTO stray
        FROM skills_fund_transactions
        WHERE status IS NULL
           OR UPPER(TRIM(status)) NOT IN
              ('PENDING', 'ALLOCATED', 'APPROVED', 'COMPLETED', 'DISBURSED');

        IF stray IS NOT NULL THEN
            RAISE EXCEPTION
                'skills_fund_transactions.status holds values with no mapping to the new enum: %. '
                    'Correct these rows to one of PENDING, ALLOCATED, APPROVED, DISBURSED and re-run. '
                    'They are not being rewritten automatically because a guessed status silently '
                    'moves money between the allocated, disbursed and pending totals.', stray;
        END IF;
    END
$$;

UPDATE skills_fund_transactions
SET status = UPPER(TRIM(status));

UPDATE skills_fund_transactions
SET status = 'DISBURSED'
WHERE status = 'COMPLETED';

ALTER TABLE skills_fund_transactions
    ALTER COLUMN status TYPE VARCHAR(16),
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE skills_fund_transactions
    ADD CONSTRAINT chk_skills_fund_transactions_status
        CHECK (status IN ('PENDING', 'ALLOCATED', 'APPROVED', 'DISBURSED'));

COMMENT ON COLUMN skills_fund_transactions.status IS
    'PENDING (requested) | ALLOCATED (earmarked) | APPROVED (signed off) | DISBURSED (money left the fund). The legacy value ''Completed'' was folded into DISBURSED — it named the same fact.';
