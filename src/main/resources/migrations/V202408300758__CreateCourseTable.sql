CREATE TABLE IF NOT EXISTS course
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)                              NOT NULL,
    code             VARCHAR(50)                               NOT NULL UNIQUE,
    description      TEXT,
    difficulty_level VARCHAR(50),
    min_age          INT CHECK (min_age >= 0),
    max_age          INT CHECK (max_age >= min_age),
    created_at       TIMESTAMP                                 NOT NULL DEFAULT current_timestamp,
    created_by       VARCHAR(50)                               NOT NULL,
    updated_at       TIMESTAMP                                 NOT NULL DEFAULT current_timestamp,
    updated_by       VARCHAR(50),
    deleted          BOOLEAN CHECK ( deleted IN (TRUE, FALSE)) NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_course_created_by ON course (created_by);
CREATE INDEX idx_course_updated_by ON course (updated_by);
CREATE INDEX idx_course_deleted ON course (deleted);