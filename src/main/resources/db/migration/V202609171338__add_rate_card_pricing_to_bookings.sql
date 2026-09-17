-- Records which approved rate card cell priced each booking; rows priced before this stay null.

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS rate_basis      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS training_format VARCHAR(20),
    ADD COLUMN IF NOT EXISTS delivery_mode   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS unit_rate       NUMERIC(12, 4);

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_rate_basis
        CHECK (rate_basis IN ('PER_HOUR', 'PER_SESSION', 'PER_DAY'));

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_training_format
        CHECK (training_format IN ('INDIVIDUAL', 'GROUP'));

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_delivery_mode
        CHECK (delivery_mode IN ('ONLINE', 'IN_PERSON', 'HYBRID'));

CREATE INDEX IF NOT EXISTS idx_bookings_student_instructor_course_start
    ON bookings (student_uuid, instructor_uuid, course_uuid, start_time);

COMMENT ON COLUMN bookings.rate_basis IS 'Unit the instructor''s approved rate was charged in';
COMMENT ON COLUMN bookings.training_format IS 'Private (INDIVIDUAL) or GROUP rate card row the booking was priced from';
COMMENT ON COLUMN bookings.delivery_mode IS 'Delivery the booking was priced for; HYBRID is priced from the in-person rates';
COMMENT ON COLUMN bookings.unit_rate IS 'The approved rate card cell, in its basis, that price_amount was computed from';
