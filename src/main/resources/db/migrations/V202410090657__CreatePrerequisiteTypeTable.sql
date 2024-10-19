CREATE TABLE IF NOT EXISTS prerequisite_type
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE UNIQUE INDEX idx_prerequisite_type_name ON prerequisite_type (name);
CREATE INDEX idx_prerequisite_type_created_by ON prerequisite_type (created_by);
CREATE INDEX idx_prerequisite_type_updated_by ON prerequisite_type (updated_by);
CREATE INDEX idx_prerequisite_type_deleted ON prerequisite_type (deleted);