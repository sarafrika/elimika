-- 202507021710__create_quiz_responses_table.sql
-- Create quiz responses table for storing individual question answers

CREATE TABLE quiz_responses
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    attempt_uuid         UUID                     NOT NULL REFERENCES quiz_attempts (uuid) ON DELETE CASCADE,
    question_uuid        UUID                     NOT NULL REFERENCES quiz_questions (uuid),
    selected_option_uuid UUID REFERENCES quiz_question_options (uuid),
    text_response        TEXT,
    points_earned        DECIMAL(5, 2)                            DEFAULT 0.00,
    is_correct           BOOLEAN,
    created_date         TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date         TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by           VARCHAR(255)             NOT NULL,
    updated_by           VARCHAR(255),
    UNIQUE (attempt_uuid, question_uuid)
);

-- Create performance indexes
CREATE INDEX idx_quiz_responses_uuid ON quiz_responses (uuid);
CREATE INDEX idx_quiz_responses_attempt_uuid ON quiz_responses (attempt_uuid);
CREATE INDEX idx_quiz_responses_question_uuid ON quiz_responses (question_uuid);
CREATE INDEX idx_quiz_responses_selected_option_uuid ON quiz_responses (selected_option_uuid);
CREATE INDEX idx_quiz_responses_created_date ON quiz_responses (created_date);