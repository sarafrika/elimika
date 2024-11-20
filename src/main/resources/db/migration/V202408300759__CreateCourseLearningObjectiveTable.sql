CREATE TABLE IF NOT EXISTS course_learning_objective
(
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT       NOT NULL,
    objective  VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id),

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_course_learning_objective_course_id ON course_learning_objective (course_id);
CREATE INDEX idx_course_learning_objective_created_by ON course_learning_objective (created_by);
CREATE INDEX idx_course_learning_objective_updated_by ON course_learning_objective (updated_by);
CREATE INDEX idx_course_learning_objective_deleted ON course_learning_objective (deleted);
