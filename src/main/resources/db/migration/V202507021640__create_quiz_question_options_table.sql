-- 202507021640__create_quiz_question_options_table.sql
-- Create quiz question options table for multiple choice questions

CREATE TABLE quiz_question_options
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    question_uuid UUID                     NOT NULL REFERENCES quiz_questions (uuid) ON DELETE CASCADE,
    option_text   TEXT                     NOT NULL,
    is_correct    BOOLEAN                                  DEFAULT false,
    display_order INTEGER                  NOT NULL        DEFAULT 1,
    created_date  TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date  TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by    VARCHAR(255)             NOT NULL,
    updated_by    VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_quiz_question_options_uuid ON quiz_question_options (uuid);
CREATE INDEX idx_quiz_question_options_question_uuid ON quiz_question_options (question_uuid);
CREATE INDEX idx_quiz_question_options_display_order ON quiz_question_options (display_order);
CREATE INDEX idx_quiz_question_options_created_date ON quiz_question_options (created_date);