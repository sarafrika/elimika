CREATE TABLE IF NOT EXISTS prerequisite
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    prerequisite_type_id   BIGINT      NOT NULL,
    course_id              BIGINT      NOT NULL,
    required_for_course_id BIGINT      NOT NULL,
    minimum_score          DOUBLE PRECISION,
    created_date             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(50) NOT NULL,
    updated_date             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             VARCHAR(50),
    deleted                BOOLEAN     NOT NULL DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (required_for_course_id) REFERENCES course (id),
    FOREIGN KEY (prerequisite_type_id) REFERENCES prerequisite_type (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE))
);

CREATE INDEX idx_prerequisite_course_id ON prerequisite (course_id);
CREATE INDEX idx_prerequisite_required_course_id ON prerequisite (required_for_course_id);
CREATE INDEX idx_prerequisite_prerequisite_type_id ON prerequisite (prerequisite_type_id);
CREATE INDEX idx_prerequisite_created_by ON prerequisite (created_by);
CREATE INDEX idx_prerequisite_updated_by ON prerequisite (updated_by);
CREATE INDEX idx_prerequisite_deleted ON prerequisite (deleted);