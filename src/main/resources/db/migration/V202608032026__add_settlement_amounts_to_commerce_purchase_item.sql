-- Per-line settlement: what actually became of the money collected on each purchased line.
--
-- Two things were wrong with the money model, and these three columns are how both become visible.
--
-- First, the platform fee was a display figure. `commerce_purchase.platform_fee_amount` was
-- computed on the whole order, stored, returned on the order response — and then never subtracted
-- from anything. Earners were credited a share of the GROSS line total. The fee now comes off the
-- top: it is apportioned across the order's lines in proportion to each line's share of the order
-- (to the cent, largest-remainder, summing exactly back to the order fee) and recorded here as
-- `platform_fee_amount`, and the revenue share is applied to what is left.
--
-- Second, a course splits revenue between its creator and its instructor and the two percentages are
-- constrained to sum to 100 — but only ONE of them is credited per purchase: a COURSE-scope sale
-- credits the creator, a CLASS-scope sale credits the class' instructor. On a 70/20 course the other
-- side's 10% was credited to nobody and existed only as a difference you could find by subtraction.
-- `retained_amount` makes it a number. This is a MEASUREMENT, not a policy change: who gets credited
-- is deliberately unchanged. The eventual home for this is a PLATFORM_UNALLOCATED_REVENUE ledger
-- account, but the double-entry ledger does not exist yet and the decision was explicitly to make
-- the figure visible first and set policy with it in hand.
--
-- The four amounts are a closed set and the check constraint holds them to it:
--     total (gross) = platform_fee_amount + credited_amount + retained_amount
-- so any line can be reconciled without recomputing anything, and a set of figures that does not
-- add up cannot be stored at all.
--
-- NO BACKFILL, AND NO CLAWBACK. Every existing row is left NULL. Wallet balances already paid out
-- were computed on gross, and people have been credited what they were credited; retroactively
-- adjusting that would be worse than the original bug. Nor can a historical retained amount be
-- derived — the fee was never apportioned, the credit was never recorded, and reconstructing either
-- from today's rates would fabricate figures that describe no transaction that ever happened. NULL
-- here means "this line was settled before settlement was recorded", which is the truth. Reporting
-- treats a NULL fee as zero for exactly this reason: those earners really did receive a share of
-- gross, and netting a fee off them retrospectively would under-report real income.
--
-- Only captured lines are ever stamped, and they are stamped by the payout module after the wallet
-- credit is applied, through the shared PurchaseSettlementRecorder seam. An uncaptured order has
-- collected nothing, so it stays NULL rather than claiming the platform retained an unpaid balance.

ALTER TABLE commerce_purchase_item
    ADD COLUMN platform_fee_amount NUMERIC(19, 4),
    ADD COLUMN credited_amount     NUMERIC(19, 4),
    ADD COLUMN retained_amount     NUMERIC(19, 4);

-- A partially-written settlement is indistinguishable from a wrong one, so all three arrive together
-- or none do; and when they do arrive they must account for the whole line.
ALTER TABLE commerce_purchase_item
    ADD CONSTRAINT chk_commerce_purchase_item_settlement_reconciles
        CHECK (
            (platform_fee_amount IS NULL AND credited_amount IS NULL AND retained_amount IS NULL)
                OR (
                platform_fee_amount IS NOT NULL
                    AND credited_amount IS NOT NULL
                    AND retained_amount IS NOT NULL
                    AND platform_fee_amount >= 0
                    AND credited_amount >= 0
                    AND retained_amount >= 0
                    AND COALESCE(total, 0) = platform_fee_amount + credited_amount + retained_amount
                )
            );

-- Reporting the platform's retained revenue is a scan over settled lines; the partial index keeps it
-- off the historical rows that carry no settlement at all.
CREATE INDEX idx_commerce_purchase_item_retained
    ON commerce_purchase_item (retained_amount)
    WHERE retained_amount IS NOT NULL;

COMMENT ON COLUMN commerce_purchase_item.platform_fee_amount IS
    'The order''s platform fee apportioned to this line in proportion to its share of the order total, taken off the top before any revenue share is applied. NULL on lines settled before the fee was charged; those earners were credited on gross and are not clawed back.';
COMMENT ON COLUMN commerce_purchase_item.credited_amount IS
    'What was actually credited to an earner''s wallet for this line — the creator on a COURSE sale, the class'' default instructor on a CLASS sale. Zero when nobody was credited (no scope, no configured revenue share, or no resolvable earner).';
COMMENT ON COLUMN commerce_purchase_item.retained_amount IS
    'Collected, not taken as platform fee, and credited to no earner — most often the other side of the course''s creator/instructor split, which no purchase scope pays out. Booked here so the gap is a figure rather than a subtraction; destined for a PLATFORM_UNALLOCATED_REVENUE ledger account once the ledger exists.';
