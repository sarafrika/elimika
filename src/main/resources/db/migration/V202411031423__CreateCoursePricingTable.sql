CREATE TABLE IF NOT EXISTS course_pricing
(
    id             BIGSERIAL PRIMARY KEY,
    course_id      BIGINT         NOT NULL,
    base_price     DECIMAL(10, 2) NOT NULL,
    discount_rate  DECIMAL(5, 2)           DEFAULT 0.00,
    discount_start TIMESTAMP,
    discount_end   TIMESTAMP,
    discount_code  VARCHAR(50),
    final_price    DECIMAL(10, 2) GENERATED ALWAYS AS ( base_price * (1 - discount_rate / 100) ) STORED,
    free           BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(50)    NOT NULL,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by     VARCHAR(50),
    deleted        BOOLEAN        NOT NULL DEFAULT FALSE,

    CONSTRAINT course_pricing_course_id_fk FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT discount_start_check CHECK (discount_start <= discount_end),
    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_course_pricing_free ON course_pricing (free);
CREATE INDEX idx_course_pricing_deleted ON course_pricing (deleted);
CREATE INDEX idx_course_pricing_course_id ON course_pricing (course_id);
CREATE INDEX idx_course_pricing_created_by ON course_pricing (created_by);
CREATE INDEX idx_course_pricing_updated_by ON course_pricing (updated_by);
CREATE INDEX idx_course_pricing_final_price ON course_pricing (final_price);