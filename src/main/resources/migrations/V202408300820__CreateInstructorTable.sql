CREATE TABLE IF NOT EXISTS instructor
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255)                              NOT NULL,
    email      VARCHAR(50)                               NOT NULL UNIQUE,
    bio        TEXT,
    created_at TIMESTAMP                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)                               NOT NULL,
    updated_at TIMESTAMP                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted    BOOLEAN CHECK ( deleted IN (TRUE, FALSE)) NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_instructor_created_by ON instructor (created_by);
CREATE INDEX idx_instructor_updated_by ON instructor (updated_by);
CREATE INDEX idx_instructor_deleted ON instructor (deleted);