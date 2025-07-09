CREATE TABLE IF NOT EXISTS instructors
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    full_name    VARCHAR(255) NOT NULL,
    bio          TEXT,
    created_date TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50)  NOT NULL,
    updated_date TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted      BOOLEAN      NOT NULL        DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_instructor_created_by ON instructors (created_by);
CREATE INDEX idx_instructor_updated_by ON instructors (updated_by);
CREATE INDEX idx_instructor_deleted ON instructors (deleted);