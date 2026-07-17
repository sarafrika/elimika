-- Draft-over-live course editing.
--
-- An edit to a published, admin-approved course is held on a shadow "draft" course row
-- rather than applied to the live row, so the live course keeps serving the last-approved
-- content while the edit awaits admin review. A draft is created as
-- status='draft', active=false, admin_approved=false, which means the existing filters on
-- the public listings (status='published' AND admin_approved=true / active=true AND
-- admin_approved=true) already exclude it without any new conditions.

ALTER TABLE courses
    ADD COLUMN parent_course_uuid UUID REFERENCES courses (uuid) ON DELETE CASCADE;

-- A live course may have at most one open draft at a time.
CREATE UNIQUE INDEX uq_courses_one_draft_per_parent
    ON courses (parent_course_uuid) WHERE parent_course_uuid IS NOT NULL;

CREATE INDEX idx_courses_parent_course_uuid ON courses (parent_course_uuid);

COMMENT ON COLUMN courses.parent_course_uuid IS
    'Set on shadow draft rows that hold an unapproved edit of the referenced live course. NULL for real courses.';

-- Promotion updates live rows in place rather than replacing them, so live uuids survive and
-- learner data keeps pointing at the right row. Every authoring table that learner data
-- references therefore needs a link from the draft copy back to its live original:
--
--   lessons               <- lesson_progress
--   lesson_contents       <- content_progress
--   quiz_questions        <- quiz_responses.question_uuid
--   quiz_question_options <- quiz_responses.selected_option_uuid
--
-- quizzes.source_quiz_uuid and assignments.source_assignment_uuid already exist and are
-- reused for the same purpose. assignment_attachments and lesson_practice_activities have no
-- learner data referencing them, so promotion may replace those rows outright.

ALTER TABLE lessons
    ADD COLUMN source_lesson_uuid UUID;

CREATE INDEX idx_lessons_source_lesson_uuid ON lessons (source_lesson_uuid);

COMMENT ON COLUMN lessons.source_lesson_uuid IS
    'On a draft lesson, the live lesson it will be promoted onto. NULL means the edit adds this lesson.';

ALTER TABLE lesson_contents
    ADD COLUMN source_content_uuid UUID;

CREATE INDEX idx_lesson_contents_source_content_uuid ON lesson_contents (source_content_uuid);

COMMENT ON COLUMN lesson_contents.source_content_uuid IS
    'On draft lesson content, the live content it will be promoted onto. NULL means the edit adds it.';

ALTER TABLE quiz_questions
    ADD COLUMN source_question_uuid UUID;

CREATE INDEX idx_quiz_questions_source_question_uuid ON quiz_questions (source_question_uuid);

COMMENT ON COLUMN quiz_questions.source_question_uuid IS
    'On a draft quiz question, the live question it will be promoted onto. NULL means the edit adds it.';

ALTER TABLE quiz_question_options
    ADD COLUMN source_option_uuid UUID;

CREATE INDEX idx_quiz_question_options_source_option_uuid ON quiz_question_options (source_option_uuid);

COMMENT ON COLUMN quiz_question_options.source_option_uuid IS
    'On a draft quiz option, the live option it will be promoted onto. NULL means the edit adds it.';
