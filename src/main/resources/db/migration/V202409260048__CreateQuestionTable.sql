CREATE TABLE IF NOT EXISTS question
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    description         TEXT,
    question_type       VARCHAR(50) NOT NULL,
    point_value         INT         NOT NULL,
    order_in_assessment INT         NOT NULL,
    assessment_id       BIGINT      NOT NULL,
    created_date          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50) NOT NULL,
    updated_date          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50),
    deleted             BOOLEAN     NOT NULL DEFAULT FALSE,

    FOREIGN KEY (assessment_id) REFERENCES assessment (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_question_assessment_id ON question (assessment_id);
CREATE INDEX idx_question_created_by ON question (created_by);
CREATE INDEX idx_question_updated_by ON question (updated_by);
CREATE INDEX idx_question_deleted ON question (deleted);