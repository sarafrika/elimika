CREATE TABLE IF NOT EXISTS lesson
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR     NOT NULL,
    description  TEXT,
    content      TEXT        NOT NULL,
    lesson_order INT         NOT NULL,
    is_published BOOLEAN              DEFAULT FALSE,
    class_id     BIGINT      NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50) NOT NULL,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,

    FOREIGN KEY (class_id) REFERENCES class (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),
    CONSTRAINT unique_lesson_order UNIQUE (class_id, lesson_order)
);

CREATE INDEX idx_lesson_class_id ON lesson (class_id);
CREATE INDEX idx_lesson_created_by ON lesson (created_by);
CREATE INDEX idx_lesson_updated_by ON lesson (updated_by);
CREATE INDEX idx_lesson_deleted ON lesson (deleted);
