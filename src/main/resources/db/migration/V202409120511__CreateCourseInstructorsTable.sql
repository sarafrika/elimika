CREATE TABLE IF NOT EXISTS course_instructor
(
    course_uuid     UUID        NOT NULL,
    uuid            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    instructor_uuid UUID        NOT NULL,
    created_date    TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_date    TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),

    PRIMARY KEY (course_uuid, instructor_uuid)
);

CREATE INDEX idx_course_instructor_course_id ON course_instructor (course_id);
CREATE INDEX idx_course_instructor_instructor_id ON course_instructor (instructor_id);
