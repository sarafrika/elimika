-- 202507021610__create_grading_levels_table.sql
-- Create grading levels table (Distinction, Merit, Pass, Fail, No Effort)

CREATE TABLE grading_levels
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name         VARCHAR(50)              NOT NULL UNIQUE,
    points       INTEGER                  NOT NULL,
    level_order  INTEGER                  NOT NULL UNIQUE,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by   VARCHAR(255)             NOT NULL,
    updated_by   VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX idx_grading_levels_uuid ON grading_levels (uuid);
CREATE INDEX idx_grading_levels_order ON grading_levels (level_order);
CREATE INDEX idx_grading_levels_name ON grading_levels (name);
CREATE INDEX idx_grading_levels_created_date ON grading_levels (created_date);