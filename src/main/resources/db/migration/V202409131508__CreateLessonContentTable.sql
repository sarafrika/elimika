CREATE TABLE IF NOT EXISTS lesson_content
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_uuid     UUID           NOT NULL,
    content_type_id BIGINT         NOT NULL,
    title           VARCHAR(255)   NOT NULL,
    content         TEXT,
    duration        DECIMAL(10, 4) NOT NULL,
    display_order   INT            NOT NULL,
    created_date    TIMESTAMP      NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)    NOT NULL,
    updated_date    TIMESTAMP      NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted         BOOLEAN        NOT NULL        DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_lesson_content_lesson_uuid ON lesson_content (lesson_uuid);
CREATE INDEX idx_lesson_content_created_by ON lesson_content (created_by);
CREATE INDEX idx_lesson_content_updated_by ON lesson_content (updated_by);