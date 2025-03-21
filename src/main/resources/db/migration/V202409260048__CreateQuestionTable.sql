CREATE TABLE IF NOT EXISTS question
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    description         TEXT,
    question_type       VARCHAR(50) NOT NULL,
    point_value         INT         NOT NULL,
    order_in_assessment INT         NOT NULL,
    assessment_uuid     UUID      NOT NULL,
    created_date        TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50) NOT NULL,
    updated_date        TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50),
    deleted             BOOLEAN     NOT NULL        DEFAULT FALSE
);

CREATE INDEX idx_question_assessment_uuid ON question (assessment_uuid);
CREATE INDEX idx_question_deleted ON question (deleted);