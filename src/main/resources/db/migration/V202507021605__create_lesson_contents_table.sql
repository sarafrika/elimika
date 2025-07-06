-- 202507021605__create_lesson_contents_table.sql
-- Create lesson contents table for storing lesson materials

CREATE TABLE lesson_contents
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_uuid       UUID                     NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    content_type_uuid UUID                     NOT NULL REFERENCES lesson_content_types (uuid),
    title             VARCHAR(255)             NOT NULL,
    description       TEXT,
    content_text      TEXT,         -- For text content
    file_url          VARCHAR(500), -- For file-based content
    file_size_bytes   BIGINT,
    mime_type         VARCHAR(100),
    display_order     INTEGER                  NOT NULL        DEFAULT 1,
    is_required       BOOLEAN                                  DEFAULT true,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date      TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_lesson_contents_uuid ON lesson_contents (uuid);
CREATE INDEX idx_lesson_contents_lesson_uuid ON lesson_contents (lesson_uuid);
CREATE INDEX idx_lesson_contents_content_type_uuid ON lesson_contents (content_type_uuid);
CREATE INDEX idx_lesson_contents_display_order ON lesson_contents (display_order);
CREATE INDEX idx_lesson_contents_created_date ON lesson_contents (created_date);