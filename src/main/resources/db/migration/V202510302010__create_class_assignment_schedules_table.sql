-- Create class_assignment_schedules table to store class-specific assignment timelines

CREATE TABLE class_assignment_schedules (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    class_definition_uuid UUID NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    lesson_uuid UUID NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    assignment_uuid UUID NOT NULL REFERENCES assignments (uuid) ON DELETE CASCADE,
    class_lesson_plan_uuid UUID REFERENCES class_lesson_plans (uuid) ON DELETE SET NULL,
    visible_at TIMESTAMP WITH TIME ZONE,
    due_at TIMESTAMP WITH TIME ZONE,
    grading_due_at TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(64),
    release_strategy VARCHAR(32) NOT NULL DEFAULT 'INHERITED',
    max_attempts INTEGER,
    instructor_uuid UUID NOT NULL REFERENCES instructors (uuid),
    notes TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    CONSTRAINT chk_assignment_release_strategy CHECK (release_strategy IN ('INHERITED', 'CUSTOM', 'CLONE')),
    CONSTRAINT uq_class_assignment_schedule UNIQUE (class_definition_uuid, assignment_uuid)
);

CREATE INDEX idx_class_assignment_schedule_uuid ON class_assignment_schedules (uuid);
CREATE INDEX idx_class_assignment_schedule_due_at ON class_assignment_schedules (due_at);
CREATE INDEX idx_class_assignment_schedule_visible_at ON class_assignment_schedules (visible_at);
CREATE INDEX idx_class_assignment_schedule_class_lesson ON class_assignment_schedules (class_definition_uuid, lesson_uuid);
