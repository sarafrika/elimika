-- 202507021750__create_certificate_templates_table.sql
-- Create certificate templates table for course completion awards

CREATE TABLE certificate_templates
(
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name                 VARCHAR(255)             NOT NULL,
    template_type        VARCHAR(50)              NOT NULL, -- course_completion, program_completion, achievement
    template_html        TEXT                     NOT NULL, -- HTML template with placeholders
    template_css         TEXT,                              -- Custom styling
    background_image_url VARCHAR(500),
    is_active            BOOLEAN                                  DEFAULT true,
    created_date         TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date         TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by           VARCHAR(255)             NOT NULL,
    updated_by           VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_certificate_templates_uuid ON certificate_templates (uuid);
CREATE INDEX idx_certificate_templates_type ON certificate_templates (template_type);
CREATE INDEX idx_certificate_templates_is_active ON certificate_templates (is_active);
CREATE INDEX idx_certificate_templates_created_date ON certificate_templates (created_date);