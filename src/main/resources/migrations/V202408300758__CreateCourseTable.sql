CREATE TABLE IF NOT EXISTS course
(
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(50)  NOT NULL UNIQUE,
    description      TEXT,
    difficulty_level VARCHAR(50),
    min_age          INT CHECK (min_age >= 0),
    max_age          INT CHECK (max_age >= min_age),
    created_at       TIMESTAMP    NOT NULL DEFAULT current_timestamp,
    created_by       VARCHAR(50)  NOT NULL,
    updated_at       TIMESTAMP    NOT NULL DEFAULT current_timestamp,
    updated_by       VARCHAR(50),
    deleted          BOOLEAN      NOT NULL
);