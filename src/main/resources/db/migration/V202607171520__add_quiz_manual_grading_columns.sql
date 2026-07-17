-- 202607171520__add_quiz_manual_grading_columns.sql
-- Support instructor manual grading of quiz text responses (short-answer, essay):
-- capture per-response feedback and grader audit, and grader audit on the attempt.

ALTER TABLE quiz_responses
    ADD COLUMN feedback   TEXT,
    ADD COLUMN graded_by  UUID,
    ADD COLUMN graded_at  TIMESTAMP WITH TIME ZONE;

ALTER TABLE quiz_attempts
    ADD COLUMN graded_by  UUID,
    ADD COLUMN graded_at  TIMESTAMP WITH TIME ZONE;
