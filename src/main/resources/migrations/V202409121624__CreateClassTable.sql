CREATE TABLE IF NOT EXISTS class
(
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(255) NOT NULL,
    scheduled_start_date       TIMESTAMP    NOT NULL,
    scheduled_end_date         TIMESTAMP    NOT NULL,
    course_id                  BIGINT       NOT NULL,
    instructor_id              BIGINT       NOT NULL,
    instructor_availability_id BIGINT       NOT NULL,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 VARCHAR(50)  NOT NULL,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 VARCHAR(50),
    deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,


    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES instructor (id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (instructor_availability_id) REFERENCES instructor_availability (id) ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),
    CONSTRAINT check_scheduled_end_date_after_scheduled_start_date CHECK (scheduled_end_date > scheduled_start_date)
);


CREATE INDEX idx_class_deleted ON class (deleted);
CREATE INDEX idx_class_course_id ON class (course_id);
CREATE INDEX idx_class_created_by ON class (created_by);
CREATE INDEX idx_class_updated_by ON class (updated_by);
CREATE INDEX idx_class_instructor_id ON class (instructor_id);
CREATE INDEX idx_class_instructor_availability_id ON class (instructor_availability_id);
