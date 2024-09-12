CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS instructor_availability
(
    id                 BIGSERIAL PRIMARY KEY,
    instructor_id      BIGINT      NOT NULL,
    availability_start DATE        NOT NULL,
    availability_end   DATE,
    day_of_week        SMALLINT    NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    time_slot_start    TIMESTAMP   NOT NULL,
    time_slot_end      TIMESTAMP   NOT NULL,
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50) NOT NULL,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    deleted            BOOLEAN     NOT NULL DEFAULT FALSE,

    FOREIGN KEY (instructor_id) REFERENCES instructor (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT time_slot_check CHECK (time_slot_start < time_slot_end),
    CONSTRAINT availability_date_check CHECK (availability_start <= availability_end),
    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),

    EXCLUDE USING gist (
        instructor_id WITH =,
        day_of_week WITH =,
        daterange(availability_start, availability_end, '[]') WITH &&,
        tsrange(time_slot_start, time_slot_end, '[]') WITH &&
        )
);