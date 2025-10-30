-- Ensure course training applications capture instructor rate cards (per hour per head) with currency.
ALTER TABLE course_training_applications
    ADD COLUMN rate_per_hour_per_head NUMERIC(12, 4),
    ADD COLUMN rate_currency VARCHAR(3);

-- Populate existing applications using the course minimum training fee and default currency (KES).
UPDATE course_training_applications cta
SET rate_per_hour_per_head = COALESCE(c.minimum_training_fee, 0),
    rate_currency          = 'KES'
FROM courses c
WHERE cta.course_uuid = c.uuid;

-- Default any remaining null currency values to KES.
UPDATE course_training_applications
SET rate_currency = 'KES'
WHERE rate_currency IS NULL;

-- Enforce not-null constraints and defaults now that existing data is backfilled.
ALTER TABLE course_training_applications
    ALTER COLUMN rate_per_hour_per_head SET NOT NULL,
    ALTER COLUMN rate_currency SET NOT NULL,
    ALTER COLUMN rate_currency SET DEFAULT 'KES';

-- Guard against negative rate submissions and enforce ISO-length currency codes.
ALTER TABLE course_training_applications
    ADD CONSTRAINT chk_course_training_applications_rate_non_negative
        CHECK (rate_per_hour_per_head >= 0),
    ADD CONSTRAINT chk_course_training_applications_currency_length
        CHECK (char_length(rate_currency) = 3);
