CREATE TABLE IF NOT EXISTS content_type
(
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name        VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_date  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(50)         NOT NULL,
    updated_date  TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(50),
    deleted     BOOLEAN             NOT NULL DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_content_type_created_by ON content_type (created_by);
CREATE INDEX idx_content_type_updated_by ON content_type (updated_by);
CREATE INDEX idx_content_type_deleted ON content_type (deleted);