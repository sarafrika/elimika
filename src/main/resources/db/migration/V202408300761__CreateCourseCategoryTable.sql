CREATE TABLE IF NOT EXISTS course_category
(
    id          BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_id   BIGINT      NOT NULL,
    category_id BIGINT      NOT NULL,
    created_date  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(50) NOT NULL,
    updated_date  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(50),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id),
    FOREIGN KEY (category_id) REFERENCES category (id),

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_course_category_course_id ON course_category (course_id);
CREATE INDEX idx_course_category_category_id ON course_category (category_id);
CREATE INDEX idx_course_category_created_by ON course_category (created_by);
CREATE INDEX idx_course_category_updated_by ON course_category (updated_by);
CREATE INDEX idx_course_category_deleted ON course_category (deleted);