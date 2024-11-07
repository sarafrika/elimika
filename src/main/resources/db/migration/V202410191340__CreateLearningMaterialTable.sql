CREATE TABLE IF NOT EXISTS learning_material
(
    id        BIGSERIAL PRIMARY KEY,
    title     VARCHAR(255) NOT NULL,
    type      VARCHAR(50),
    url       TEXT,
    course_id BIGINT       NOT NULL,
    lesson_id BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (lesson_id) REFERENCES lesson (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_reading_material_course_id ON learning_material (course_id);
CREATE INDEX idx_reading_material_lesson_id ON learning_material (lesson_id);
CREATE INDEX idx_reading_material_created_by ON learning_material (created_by);
CREATE INDEX idx_reading_material_updated_by ON learning_material (updated_by);
CREATE INDEX idx_reading_material_deleted ON learning_material (deleted);