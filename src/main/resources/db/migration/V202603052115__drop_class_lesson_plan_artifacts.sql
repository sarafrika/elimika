ALTER TABLE class_assignment_schedules
    DROP COLUMN IF EXISTS class_lesson_plan_uuid;

ALTER TABLE class_quiz_schedules
    DROP COLUMN IF EXISTS class_lesson_plan_uuid;

DROP TABLE IF EXISTS class_lesson_plans;
