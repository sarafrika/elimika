-- A seat is held from checkout until the payment resolves, so a learner cannot be charged for a
-- place another buyer takes in the meantime.

ALTER TABLE class_enrollments
    ADD COLUMN IF NOT EXISTS reserved_until TIMESTAMP;

-- The original constraint listed only ENROLLED, ATTENDED, ABSENT and CANCELLED. WAITLISTED was
-- added to the application enum but never to this check, so every waitlist write would have been
-- rejected by the database; it is included here rather than left as a second latent failure.
ALTER TABLE class_enrollments
    DROP CONSTRAINT IF EXISTS enrollments_status_check;

ALTER TABLE class_enrollments
    DROP CONSTRAINT IF EXISTS class_enrollments_status_check;

ALTER TABLE class_enrollments
    ADD CONSTRAINT class_enrollments_status_check
        CHECK (status IN ('RESERVED', 'ENROLLED', 'WAITLISTED', 'ATTENDED', 'ABSENT', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_class_enrollments_reserved_until
    ON class_enrollments (reserved_until)
    WHERE reserved_until IS NOT NULL;

COMMENT ON COLUMN class_enrollments.reserved_until
    IS 'When an unpaid seat hold lapses. Set at checkout and cleared once the payment is captured; a hold that outlives its payment is a seat nobody can buy.';
