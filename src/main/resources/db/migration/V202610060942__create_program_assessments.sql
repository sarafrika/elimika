-- A program's weighted assessment components (e.g. Attendance 10, Practical 40, Quiz 20, Performance 30).
-- A member course's component links to the program component it feeds, and the program grade
-- averages those course scores per program component.
CREATE TABLE IF NOT EXISTS program_assessments
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    program_uuid      UUID          NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
    title             VARCHAR(255)  NOT NULL,
    assessment_type   VARCHAR(50)   NOT NULL,
    description       TEXT,
    weight_percentage NUMERIC(5, 2) NOT NULL,
    rubric_uuid       UUID REFERENCES assessment_rubrics (uuid) ON DELETE SET NULL,
    is_required       BOOLEAN       NOT NULL DEFAULT true,
    active            BOOLEAN       NOT NULL DEFAULT true,
    created_date      TIMESTAMP     NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by        VARCHAR(255)  NOT NULL,
    updated_date      TIMESTAMP,
    updated_by        VARCHAR(255),
    CONSTRAINT chk_program_assessments_weight CHECK (weight_percentage > 0 AND weight_percentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_program_assessments_program ON program_assessments (program_uuid);

ALTER TABLE course_assessments
    ADD COLUMN IF NOT EXISTS program_assessment_uuid UUID REFERENCES program_assessments (uuid) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_course_assessments_program_assessment ON course_assessments (program_assessment_uuid);
