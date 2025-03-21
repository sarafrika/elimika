CREATE TABLE IF NOT EXISTS assessment
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title         VARCHAR(255) NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    description   TEXT,
    maximum_score INT          NOT NULL,
    passing_score INT          NOT NULL,
    due_date      TIMESTAMP    NOT NULL,
    time_limit    INT          NOT NULL,
    course_uuid   UUID         NOT NULL,
    lesson_uuid   UUID,
    created_date  TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50)  NOT NULL,
    updated_date  TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    deleted       BOOLEAN      NOT NULL        DEFAULT FALSE,

    CONSTRAINT deleted_check CHECK (deleted IN (TRUE, FALSE)),
    CONSTRAINT max_score_check CHECK (maximum_score >= 0),
    CONSTRAINT passing_score_check CHECK (passing_score >= 0 AND passing_score <= maximum_score),
    CONSTRAINT time_limit_check CHECK (time_limit >= 0)
);

CREATE INDEX IF NOT EXISTS idx_assessment_course_uuid ON assessment (course_uuid);
CREATE INDEX IF NOT EXISTS idx_assessment_lesson_uuid ON assessment (lesson_uuid);
CREATE INDEX IF NOT EXISTS idx_assessment_deleted ON assessment (deleted);
