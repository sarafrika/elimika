-- 202507021655__create_lesson_progress_table.sql
-- Create lesson progress table for tracking student progress through lessons

CREATE TABLE lesson_progress
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    enrollment_uuid    UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    lesson_uuid        UUID                     NOT NULL REFERENCES lessons (uuid),
    status             VARCHAR(20)              NOT NULL        DEFAULT 'not_started' CHECK (status IN ('not_started', 'in_progress', 'completed')),
    started_at         TIMESTAMP WITH TIME ZONE,
    completed_at       TIMESTAMP WITH TIME ZONE,
    time_spent_minutes INTEGER                                  DEFAULT 0,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255),
    UNIQUE (enrollment_uuid, lesson_uuid)
);

-- Create performance indexes
CREATE INDEX idx_lesson_progress_uuid ON lesson_progress (uuid);
CREATE INDEX idx_lesson_progress_enrollment_uuid ON lesson_progress (enrollment_uuid);
CREATE INDEX idx_lesson_progress_lesson_uuid ON lesson_progress (lesson_uuid);
CREATE INDEX idx_lesson_progress_status ON lesson_progress (status);
CREATE INDEX idx_lesson_progress_created_date ON lesson_progress (created_date);