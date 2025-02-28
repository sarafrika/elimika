CREATE TABLE IF NOT EXISTS course_instructor
(
    course_id     BIGINT NOT NULL,
    uuid UUID NOT NULL UNIQUE   DEFAULT gen_random_uuid(),
    instructor_id BIGINT NOT NULL,
    created_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)  NOT NULL,
    updated_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),

    PRIMARY KEY (course_id, instructor_id),
    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES instructor (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_course_instructor_course_id ON course_instructor (course_id);
CREATE INDEX idx_course_instructor_instructor_id ON course_instructor (instructor_id);
