-- 202507021700__create_content_progress_table.sql
-- Create content progress table for tracking access to individual content items

CREATE TABLE content_progress
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    enrollment_uuid   UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    content_uuid      UUID                     NOT NULL REFERENCES lesson_contents (uuid),
    is_accessed       BOOLEAN                                  DEFAULT false,
    is_completed      BOOLEAN                                  DEFAULT false,
    access_count      INTEGER                                  DEFAULT 0,
    first_accessed_at TIMESTAMP WITH TIME ZONE,
    last_accessed_at  TIMESTAMP WITH TIME ZONE,
    completed_at      TIMESTAMP WITH TIME ZONE,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date      TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    UNIQUE (enrollment_uuid, content_uuid)
);

-- Create performance indexes
CREATE INDEX idx_content_progress_uuid ON content_progress (uuid);
CREATE INDEX idx_content_progress_enrollment_uuid ON content_progress (enrollment_uuid);
CREATE INDEX idx_content_progress_content_uuid ON content_progress (content_uuid);
CREATE INDEX idx_content_progress_is_completed ON content_progress (is_completed);
CREATE INDEX idx_content_progress_created_date ON content_progress (created_date);