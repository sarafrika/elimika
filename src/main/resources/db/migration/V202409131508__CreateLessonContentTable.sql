CREATE TABLE IF NOT EXISTS lesson_content
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_id       BIGINT       NOT NULL,
    content_type_id BIGINT       NOT NULL,
    title           VARCHAR(255) NOT NULL,
    content         TEXT,
    duration        INT,
    display_order   INT          NOT NULL,
    created_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)  NOT NULL,
    updated_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (lesson_id) REFERENCES lesson (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (content_type_id) REFERENCES content_type (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_lesson_content_lesson_id ON lesson_content (lesson_id);
CREATE INDEX idx_lesson_content_created_by ON lesson_content (created_by);
CREATE INDEX idx_lesson_content_updated_by ON lesson_content (updated_by);