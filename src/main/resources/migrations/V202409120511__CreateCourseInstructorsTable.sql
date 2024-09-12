CREATE TABLE IF NOT EXISTS course_instructors
(
    course_id     BIGINT NOT NULL,
    instructor_id BIGINT NOT NULL,

    PRIMARY KEY (course_id, instructor_id),
    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (instructor_id) REFERENCES instructor (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_course_instructor_course_id ON course_instructors (course_id);
CREATE INDEX idx_course_instructor_instructor_id ON course_instructors (instructor_id);
