-- 202507021600__create_lesson_content_types_table.sql
-- Create content types table for lesson content (PDF, Text, Image, Video, Audio)

CREATE TABLE lesson_content_types
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name             VARCHAR(50)              NOT NULL UNIQUE,
    mime_types       TEXT[], -- Array of supported MIME types
    max_file_size_mb INTEGER,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255)
);

-- Create indexes
CREATE INDEX idx_content_types_uuid ON content_types (uuid);
CREATE INDEX idx_content_types_name ON content_types (name);
CREATE INDEX idx_content_types_created_date ON content_types (created_date);