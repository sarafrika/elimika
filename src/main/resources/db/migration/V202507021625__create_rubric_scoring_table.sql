-- 202507021625__create_rubric_scoring_table.sql
-- Create rubric scoring table for descriptions at each grading level

CREATE TABLE rubric_scoring
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    criteria_uuid      UUID                     NOT NULL REFERENCES rubric_criteria (uuid) ON DELETE CASCADE,
    grading_level_uuid UUID                     NOT NULL REFERENCES grading_levels (uuid),
    description        TEXT                     NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255),
    UNIQUE (criteria_uuid, grading_level_uuid)
);

-- Create performance indexes
CREATE INDEX idx_rubric_scoring_uuid ON rubric_scoring (uuid);
CREATE INDEX idx_rubric_scoring_criteria_uuid ON rubric_scoring (criteria_uuid);
CREATE INDEX idx_rubric_scoring_grading_level_uuid ON rubric_scoring (grading_level_uuid);
CREATE INDEX idx_rubric_scoring_created_date ON rubric_scoring (created_date);