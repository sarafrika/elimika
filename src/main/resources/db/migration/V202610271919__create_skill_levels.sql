-- The one platform-wide scale that turns a learner's skill evidence into a named level.
CREATE TABLE IF NOT EXISTS skill_levels
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name               VARCHAR(60)   NOT NULL,
    min_percentage     NUMERIC(5, 2) NOT NULL,
    counts_as_achieved BOOLEAN       NOT NULL DEFAULT false,
    display_order      INTEGER       NOT NULL DEFAULT 0,
    created_date       TIMESTAMP     NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by         VARCHAR(255)  NOT NULL,
    updated_date       TIMESTAMP,
    updated_by         VARCHAR(255),
    CONSTRAINT uq_skill_levels_min UNIQUE (min_percentage),
    CONSTRAINT chk_skill_levels_min CHECK (min_percentage >= 0 AND min_percentage <= 100)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_skill_levels_name ON skill_levels (LOWER(name));

INSERT INTO skill_levels (name, min_percentage, counts_as_achieved, display_order, created_by)
VALUES ('Not achieved', 0, false, 1, 'system'),
       ('Developing', 50, false, 2, 'system'),
       ('Achieved', 70, true, 3, 'system')
ON CONFLICT DO NOTHING;
