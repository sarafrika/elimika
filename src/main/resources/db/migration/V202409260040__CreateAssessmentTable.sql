CREATE TABLE IF NOT EXISTS assessment
(
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    description   TEXT,
    maximum_score INT          NOT NULL,
    passing_score INT          NOT NULL,
    due_date      TIMESTAMP    NOT NULL,
    time_limit    INT          NOT NULL,
    course_id     BIGINT       NOT NULL,
    lesson_id     BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50)  NOT NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (lesson_id) REFERENCES lesson (id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),
    CONSTRAINT max_score_check CHECK (maximum_score >= 0),
    CONSTRAINT passing_score_check CHECK (passing_score >= 0 AND passing_score <= maximum_score),
    CONSTRAINT time_limit_check CHECK (time_limit >= 0)
);

CREATE INDEX IF NOT EXISTS idx_assessment_course_id ON assessment (course_id);
CREATE INDEX IF NOT EXISTS idx_assessment_lesson_id ON assessment (lesson_id);
CREATE INDEX IF NOT EXISTS idx_assessment_created_by ON assessment (created_by);
CREATE INDEX IF NOT EXISTS idx_assessment_updated_by ON assessment (updated_by);
CREATE INDEX IF NOT EXISTS idx_assessment_deleted ON assessment (deleted);
