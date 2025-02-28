CREATE TABLE IF NOT EXISTS lesson_resource
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_id     BIGINT       NOT NULL,
    resource_url  TEXT         NOT NULL,
    title         VARCHAR(255) NOT NULL,
    display_order INT          NOT NULL,
    created_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50)  NOT NULL,
    updated_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (lesson_id) REFERENCES lesson (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_lesson_resource_lesson_id ON lesson_resource (lesson_id);
CREATE INDEX idx_lesson_resource_created_by ON lesson_resource (created_by);
CREATE INDEX idx_lesson_resource_updated_by ON lesson_resource (updated_by);
CREATE INDEX idx_lesson_resource_deleted ON lesson_resource (deleted);