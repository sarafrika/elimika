-- Creates booking table to store instructor/course bookings with payment metadata and availability locks.

CREATE TABLE IF NOT EXISTS bookings
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID                  NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid          UUID                  NOT NULL,
    course_uuid           UUID                  NOT NULL,
    instructor_uuid       UUID                  NOT NULL,
    start_time            TIMESTAMP             NOT NULL,
    end_time              TIMESTAMP             NOT NULL,
    status                VARCHAR(32)           NOT NULL,
    hold_expires_at       TIMESTAMP,
    price_amount          NUMERIC(12, 2),
    currency              VARCHAR(3),
    payment_session_id    VARCHAR(128),
    payment_reference     VARCHAR(128),
    payment_engine        VARCHAR(64),
    purpose               VARCHAR(500),
    availability_block_uuid UUID,
    created_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(255)          NOT NULL,
    updated_by            VARCHAR(255),

    CONSTRAINT bookings_status_check CHECK (status IN ('PAYMENT_REQUIRED', 'CONFIRMED', 'CANCELLED', 'PAYMENT_FAILED', 'EXPIRED')),
    CONSTRAINT bookings_time_check CHECK (start_time < end_time)
);

CREATE INDEX idx_bookings_student ON bookings (student_uuid);
CREATE INDEX idx_bookings_course ON bookings (course_uuid);
CREATE INDEX idx_bookings_instructor ON bookings (instructor_uuid);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_start_time ON bookings (start_time);
CREATE INDEX idx_bookings_hold_expires ON bookings (hold_expires_at);

COMMENT ON TABLE bookings IS 'Stores student bookings for instructor sessions with payment metadata and availability holds';
COMMENT ON COLUMN bookings.availability_block_uuid IS 'UUID of the availability slot blocked for this booking';
