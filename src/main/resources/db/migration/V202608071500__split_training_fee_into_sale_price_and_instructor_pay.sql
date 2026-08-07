-- Splits the single class fee into the two numbers it was actually being used as.
--
-- `training_fee` was one column serving three unrelated consumers:
--   * the student price     — commerce reads it into the purchasable variant's unit amount
--   * the instructor's pay  — the payout module accrues it verbatim into instructor_obligations
--   * a rate-card assertion — class creation demanded it equal the approved rate exactly
--
-- Because charge and pay were the same value, an organisation could not take a margin: the equality
-- assertion rejected any difference, and had that assertion simply been relaxed the instructor would
-- have been accrued the organisation's own sticker price. The margin was not merely disallowed, it
-- was unrepresentable.
--
-- A job posting now declares both numbers up front — the organisation does not know who it will hire
-- when it posts, which is precisely why it must state what it will pay. The applicant's approved rate
-- card becomes the floor they will accept, and the margin is sale_price - instructor_pay.
--
-- The rename is deliberate rather than additive. `training_fee` currently drives the student price, so
-- leaving that name on the semantically-changed field is exactly how a later reader takes the wrong
-- number — and here the wrong number is somebody's pay.
--
-- BACKFILL: instructor_pay is seeded from the existing fee on every row, so historical classes keep
-- accruing exactly what they accrued before (a zero margin). Leaving it NULL would be silent and
-- expensive: the payout accrual skips any class whose pay is null or non-positive, so untouched rows
-- would simply stop paying instructors with nothing in the logs to say so.

ALTER TABLE class_definitions
    ADD COLUMN instructor_pay NUMERIC(12, 2);

UPDATE class_definitions
SET instructor_pay = training_fee
WHERE training_fee IS NOT NULL;

ALTER TABLE class_definitions
    RENAME COLUMN training_fee TO sale_price;

ALTER TABLE class_definitions
    RENAME CONSTRAINT chk_class_definitions_training_fee_non_negative
        TO chk_class_definitions_sale_price_non_negative;

ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_instructor_pay_non_negative
        CHECK (instructor_pay IS NULL OR instructor_pay >= 0);

-- The organisation may keep a margin but may not sell below what it pays out.
ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_margin_non_negative
        CHECK (instructor_pay IS NULL OR sale_price IS NULL OR instructor_pay <= sale_price);

ALTER TABLE class_marketplace_jobs
    ADD COLUMN instructor_pay NUMERIC(12, 2);

UPDATE class_marketplace_jobs
SET instructor_pay = training_fee
WHERE training_fee IS NOT NULL;

ALTER TABLE class_marketplace_jobs
    RENAME COLUMN training_fee TO sale_price;

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_sale_price_non_negative
        CHECK (sale_price IS NULL OR sale_price >= 0);

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_instructor_pay_non_negative
        CHECK (instructor_pay IS NULL OR instructor_pay >= 0);

ALTER TABLE class_marketplace_jobs
    ADD CONSTRAINT chk_class_marketplace_jobs_margin_non_negative
        CHECK (instructor_pay IS NULL OR sale_price IS NULL OR instructor_pay <= sale_price);

COMMENT ON COLUMN class_definitions.sale_price
    IS 'What a learner is charged per session for this class. Drives the purchasable variant unit amount.';

COMMENT ON COLUMN class_definitions.instructor_pay
    IS 'What the organisation owes the instructor per session. Accrued into instructor_obligations on session completion. sale_price - instructor_pay is the organisation margin.';

COMMENT ON COLUMN class_marketplace_jobs.sale_price
    IS 'Advertised price per session a learner will be charged once the class exists; copied to the class definition when the class is created.';

COMMENT ON COLUMN class_marketplace_jobs.instructor_pay
    IS 'Per-session pay the organisation offers the eventual instructor. An applicant is assignable only when this is at least their approved rate card amount.';
