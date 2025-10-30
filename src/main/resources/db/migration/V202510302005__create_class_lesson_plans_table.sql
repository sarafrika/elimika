-- Create class_lesson_plans table for managing lesson ordering and metadata per class

CREATE TABLE class_lesson_plans (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    class_definition_uuid UUID NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    lesson_uuid UUID NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    scheduled_start TIMESTAMP WITH TIME ZONE,
    scheduled_end TIMESTAMP WITH TIME ZONE,
    scheduled_instance_uuid UUID REFERENCES scheduled_instances (uuid) ON DELETE SET NULL,
    instructor_uuid UUID REFERENCES instructors (uuid) ON DELETE SET NULL,
    notes TEXT,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    CONSTRAINT uq_class_lesson_plan UNIQUE (class_definition_uuid, lesson_uuid)
);

CREATE INDEX idx_class_lesson_plan_uuid ON class_lesson_plans (uuid);
CREATE INDEX idx_class_lesson_plan_class_lesson ON class_lesson_plans (class_definition_uuid, lesson_uuid);
CREATE INDEX idx_class_lesson_plan_scheduled_instance ON class_lesson_plans (scheduled_instance_uuid);
