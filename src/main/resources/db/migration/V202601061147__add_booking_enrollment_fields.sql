-- Adds booking links to scheduled instances and enrollments; expands status check for accepted/confirmed.

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS scheduled_instance_uuid UUID,
    ADD COLUMN IF NOT EXISTS enrollment_uuid UUID;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS bookings_status_check;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_status_check CHECK (status IN (
        'PAYMENT_REQUIRED',
        'CONFIRMED',
        'CANCELLED',
        'PAYMENT_FAILED',
        'EXPIRED',
        'ACCEPTED',
        'DECLINED',
        'ACCEPTED_CONFIRMED'
    ));

COMMENT ON COLUMN bookings.scheduled_instance_uuid IS 'UUID of the scheduled class instance created for this booking';
COMMENT ON COLUMN bookings.enrollment_uuid IS 'UUID of the class enrollment created for this booking';
