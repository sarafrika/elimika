CREATE TABLE IF NOT EXISTS lesson
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title        VARCHAR     NOT NULL,
    description  TEXT,
    lesson_order INT         NOT NULL,
    is_published BOOLEAN                     DEFAULT FALSE,
    course_id    BIGINT      NOT NULL,
    created_date TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50) NOT NULL,
    updated_date TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted      BOOLEAN     NOT NULL        DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_lesson_course_id ON lesson (course_id);
CREATE INDEX idx_lesson_created_by ON lesson (created_by);
CREATE INDEX idx_lesson_updated_by ON lesson (updated_by);
CREATE INDEX idx_lesson_deleted ON lesson (deleted);
