CREATE TABLE IF NOT EXISTS category
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(50)  NOT NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(50),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),
    CONSTRAINT unique_category_name UNIQUE (name)
);

CREATE INDEX idx_category_created_by ON category (created_by);
CREATE INDEX idx_category_updated_by ON category (updated_by);
CREATE INDEX idx_category_deleted ON category (deleted);