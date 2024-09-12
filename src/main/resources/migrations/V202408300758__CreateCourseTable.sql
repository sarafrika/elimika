CREATE TABLE IF NOT EXISTS course
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    description      TEXT,
    difficulty_level VARCHAR(50),
    min_age          INT,
    max_age          INT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50)  NOT NULL,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(50),
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT min_age_check CHECK (min_age >= 0),
    CONSTRAINT max_age_check CHECK (max_age >= min_age),
    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_course_created_by ON course (created_by);
CREATE INDEX idx_course_updated_by ON course (updated_by);
CREATE INDEX idx_course_deleted ON course (deleted);