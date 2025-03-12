CREATE TABLE IF NOT EXISTS availability_pattern
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    instructor_id BIGINT REFERENCES instructor (id),
    pattern_type  VARCHAR(20) NOT NULL CHECK (pattern_type IN ('weekly', 'monthly', 'custom')),
    start_date    DATE        NOT NULL,
    end_date      DATE,
    created_date  TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_date  TIMESTAMP,
    updated_by    VARCHAR(50),
    deleted       BOOLEAN     NOT NULL        DEFAULT FALSE,

    CONSTRAINT valid_date_range_check CHECK (end_date IS NULL OR start_date < end_date),
    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE TABLE IF NOT EXISTS weekly_availability
(
    id          BIGSERIAL PRIMARY KEY,
    pattern_id  BIGINT REFERENCES availability_pattern (id),
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN
                                            ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday',
                                             'sunday')),
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,

    CONSTRAINT valid_time_range_check CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS monthly_availability
(
    id           BIGSERIAL PRIMARY KEY,
    pattern_id   BIGINT REFERENCES availability_pattern (id),
    day_of_month INTEGER NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    start_time   TIME    NOT NULL,
    end_time     TIME    NOT NULL,

    CONSTRAINT valid_time_range_check CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS custom_availability
(
    id            BIGSERIAL PRIMARY KEY,
    pattern_id    BIGINT REFERENCES availability_pattern (id),
    specific_date DATE NOT NULL,
    start_time    TIME NOT NULL,
    end_time      TIME NOT NULL,

    CONSTRAINT valid_time_range_check CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS availability_exception
(
    id             BIGSERIAL PRIMARY KEY,
    instructor_id  BIGINT REFERENCES instructor (id),
    exception_date DATE        NOT NULL,
    start_time     TIME,
    end_time       TIME,
    reason         VARCHAR(255),
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50) NOT NULL,
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(50),
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT valid_time_range_check CHECK (end_time IS NULL OR start_time < end_time),
    CONSTRAINT valid_date_range_check CHECK (exception_date >= CURRENT_DATE),
    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_availability_pattern_instructor ON availability_pattern (instructor_id);
CREATE INDEX idx_availability_pattern_created_by ON availability_pattern (created_by);
CREATE INDEX idx_availability_pattern_updated_by ON availability_pattern (updated_by);
CREATE INDEX idx_availability_pattern_deleted ON availability_pattern (deleted);
CREATE INDEX idx_weekly_availability_pattern ON weekly_availability (pattern_id);
CREATE INDEX idx_monthly_availability_pattern ON monthly_availability (pattern_id);
CREATE INDEX idx_custom_availability_pattern ON custom_availability (pattern_id);
CREATE INDEX idx_availability_exception_instructor ON availability_exception (instructor_id);
CREATE INDEX idx_availability_exception_created_by ON availability_exception (created_by);
CREATE INDEX idx_availability_exception_updated_by ON availability_exception (updated_by);
CREATE INDEX idx_availability_exception_deleted ON availability_exception (deleted);

