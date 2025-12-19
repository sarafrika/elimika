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
        'DECLINED'
    ));
