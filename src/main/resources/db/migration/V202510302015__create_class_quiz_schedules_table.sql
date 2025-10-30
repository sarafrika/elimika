-- Create class_quiz_schedules table to store class-specific quiz availability and overrides

CREATE TABLE class_quiz_schedules (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    class_definition_uuid UUID NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    lesson_uuid UUID NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    quiz_uuid UUID NOT NULL REFERENCES quizzes (uuid) ON DELETE CASCADE,
    class_lesson_plan_uuid UUID REFERENCES class_lesson_plans (uuid) ON DELETE SET NULL,
    visible_at TIMESTAMP WITH TIME ZONE,
    due_at TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(64),
    release_strategy VARCHAR(32) NOT NULL DEFAULT 'INHERITED',
    time_limit_override INTEGER,
    attempt_limit_override INTEGER,
    passing_score_override DECIMAL(5, 2),
    instructor_uuid UUID NOT NULL REFERENCES instructors (uuid),
    notes TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    CONSTRAINT chk_quiz_release_strategy CHECK (release_strategy IN ('INHERITED', 'CUSTOM', 'CLONE')),
    CONSTRAINT uq_class_quiz_schedule UNIQUE (class_definition_uuid, quiz_uuid)
);

CREATE INDEX idx_class_quiz_schedule_uuid ON class_quiz_schedules (uuid);
CREATE INDEX idx_class_quiz_schedule_due_at ON class_quiz_schedules (due_at);
CREATE INDEX idx_class_quiz_schedule_visible_at ON class_quiz_schedules (visible_at);
CREATE INDEX idx_class_quiz_schedule_class_lesson ON class_quiz_schedules (class_definition_uuid, lesson_uuid);
