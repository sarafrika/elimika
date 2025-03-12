CREATE TABLE IF NOT EXISTS course
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    description      TEXT,
    thumbnail_url    VARCHAR(255),
    duration_hours   DECIMAL(10, 2),
    difficulty_level VARCHAR(50),
    is_free          BOOLEAN      NOT NULL        DEFAULT FALSE,
    original_price   DECIMAL(10, 2),
    sale_price       DECIMAL(10, 2),
    min_age          INT,
    max_age          INT,
    created_date     TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50)  NOT NULL,
    updated_date     TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(50),
    deleted          BOOLEAN      NOT NULL        DEFAULT FALSE,

    CONSTRAINT min_age_check CHECK (min_age >= 0),
    CONSTRAINT max_age_check CHECK (max_age >= min_age),
    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),
    CONSTRAINT sale_price_check CHECK (sale_price <= original_price),
    CONSTRAINT price_check CHECK ((is_free = TRUE AND original_price IS NULL AND sale_price IS NULL) OR
                                  (is_free = FALSE AND original_price IS NOT NULL))
);

CREATE INDEX idx_course_created_by ON course (created_by);
CREATE INDEX idx_course_updated_by ON course (updated_by);
CREATE INDEX idx_course_deleted ON course (deleted);
CREATE INDEX idx_course_price ON course (is_free, original_price, sale_price);
CREATE INDEX idx_course_difficulty ON course (difficulty_level);