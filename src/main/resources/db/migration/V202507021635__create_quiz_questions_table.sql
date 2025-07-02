-- 202507021635__create_quiz_questions_table.sql
-- Create quiz questions table

CREATE TABLE quiz_questions
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    quiz_uuid     UUID                     NOT NULL REFERENCES quizzes (uuid) ON DELETE CASCADE,
    question_text TEXT                     NOT NULL,
    question_type VARCHAR(20)              NOT NULL CHECK (question_type IN
                                                           ('multiple_choice', 'true_false', 'short_answer', 'essay')),
    points        DECIMAL(5, 2)            NOT NULL        DEFAULT 1.00,
    display_order INTEGER                  NOT NULL        DEFAULT 1,
    created_date  TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date  TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by    VARCHAR(255)             NOT NULL,
    updated_by    VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_quiz_questions_uuid ON quiz_questions (uuid);
CREATE INDEX idx_quiz_questions_quiz_uuid ON quiz_questions (quiz_uuid);
CREATE INDEX idx_quiz_questions_display_order ON quiz_questions (display_order);
CREATE INDEX idx_quiz_questions_created_date ON quiz_questions (created_date);