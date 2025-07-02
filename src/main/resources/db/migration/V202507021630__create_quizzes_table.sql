-- 202507021630__create_quizzes_table.sql
-- Create quizzes table for lesson assessments

CREATE TABLE quizzes
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_uuid        UUID                     NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    title              VARCHAR(255)             NOT NULL,
    description        TEXT,
    instructions       TEXT,
    time_limit_minutes INTEGER,
    attempts_allowed   INTEGER                                  DEFAULT 1,
    passing_score      DECIMAL(5, 2)                            DEFAULT 70.00,
    rubric_uuid        UUID REFERENCES assessment_rubrics (uuid),
    is_published       BOOLEAN                                  DEFAULT false,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_quizzes_uuid ON quizzes (uuid);
CREATE INDEX idx_quizzes_lesson_uuid ON quizzes (lesson_uuid);
CREATE INDEX idx_quizzes_rubric_uuid ON quizzes (rubric_uuid);
CREATE INDEX idx_quizzes_is_published ON quizzes (is_published);
CREATE INDEX idx_quizzes_created_date ON quizzes (created_date);