CREATE TABLE IF NOT EXISTS course_category
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid   UUID        NOT NULL,
    category_uuid UUID        NOT NULL,
    created_date  TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_date  TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    deleted       BOOLEAN     NOT NULL        DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_course_category_course_uuid ON course_category (course_uuid);
CREATE INDEX idx_course_category_category_uuid ON course_category (category_uuid);
CREATE INDEX idx_course_category_created_by ON course_category (created_by);
CREATE INDEX idx_course_category_updated_by ON course_category (updated_by);
CREATE INDEX idx_course_category_deleted ON course_category (deleted);
CREATE UNIQUE INDEX idx_course_category_course_uuid_category_uuid ON course_category (course_uuid, category_uuid);