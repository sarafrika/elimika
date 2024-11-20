CREATE TYPE prerequisite_group_group_type AS ENUM ('AND', 'OR');

CREATE TABLE IF NOT EXISTS prerequisite_group
(
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT                        NOT NULL,
    group_type prerequisite_group_group_type NOT NULL,
    created_at TIMESTAMP                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)                   NOT NULL,
    updated_at TIMESTAMP                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted    BOOLEAN                       NOT NULL DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_prerequisite_group_course_id ON prerequisite_group (course_id);
CREATE INDEX idx_prerequisite_group_created_by ON prerequisite_group (created_by);
CREATE INDEX idx_prerequisite_group_updated_by ON prerequisite_group (updated_by);
CREATE INDEX idx_prerequisite_group_deleted ON prerequisite_group (deleted);