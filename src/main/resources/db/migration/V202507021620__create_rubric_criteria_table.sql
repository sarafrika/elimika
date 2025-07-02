-- 202507021620__create_rubric_criteria_table.sql
-- Create rubric criteria table for assessment components (Technique, Tonal Quality, etc.)

CREATE TABLE rubric_criteria
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    rubric_uuid    UUID                     NOT NULL REFERENCES assessment_rubrics (uuid) ON DELETE CASCADE,
    component_name VARCHAR(100)             NOT NULL, -- Technique, Tonal Quality, Rhythm, etc.
    description    TEXT,
    display_order  INTEGER                  NOT NULL        DEFAULT 1,
    created_date   TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date   TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by     VARCHAR(255)             NOT NULL,
    updated_by     VARCHAR(255)
);

-- Create performance indexes
CREATE INDEX idx_rubric_criteria_uuid ON rubric_criteria (uuid);
CREATE INDEX idx_rubric_criteria_rubric_uuid ON rubric_criteria (rubric_uuid);
CREATE INDEX idx_rubric_criteria_display_order ON rubric_criteria (display_order);
CREATE INDEX idx_rubric_criteria_created_date ON rubric_criteria (created_date);