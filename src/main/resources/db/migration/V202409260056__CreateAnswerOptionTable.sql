CREATE TABLE IF NOT EXISTS answer_option
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    question_uuid     BIGINT       NOT NULL,
    option_text       VARCHAR(255) NOT NULL,
    correct           BOOLEAN      NOT NULL        DEFAULT FALSE,
    order_in_question INT          NOT NULL,

    created_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50)  NOT NULL,
    updated_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN      NOT NULL        DEFAULT FALSE
);

CREATE INDEX idx_answer_option_question_id ON answer_option (question_uuid);
CREATE INDEX idx_answer_option_deleted ON answer_option (deleted);
