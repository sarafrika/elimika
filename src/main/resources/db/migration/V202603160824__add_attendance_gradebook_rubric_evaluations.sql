-- Extend the weighted gradebook for class-attendance sync and rubric-backed line-item evaluations.

ALTER TABLE course_assessments
ADD COLUMN sync_class_attendance BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE course_assessment_line_items
ADD COLUMN scheduled_instance_uuid UUID;

CREATE INDEX idx_course_assessment_line_items_scheduled_instance_uuid
ON course_assessment_line_items (scheduled_instance_uuid);

CREATE TABLE course_assessment_line_item_rubric_evaluations
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    line_item_uuid    UUID                     NOT NULL REFERENCES course_assessment_line_items (uuid) ON DELETE CASCADE,
    enrollment_uuid   UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    rubric_uuid       UUID                     NOT NULL REFERENCES assessment_rubrics (uuid),
    status            VARCHAR(20)              NOT NULL CHECK (status IN ('pending', 'completed')),
    attendance_status VARCHAR(20)                       CHECK (attendance_status IN ('attended', 'absent')),
    score             DECIMAL(7, 2),
    max_score         DECIMAL(7, 2),
    percentage        DECIMAL(5, 2),
    comments          TEXT,
    graded_at         TIMESTAMP WITH TIME ZONE,
    graded_by_uuid    UUID REFERENCES users (uuid),
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date      TIMESTAMP WITH TIME ZONE          DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    CONSTRAINT uq_course_assessment_line_item_rubric_evaluation UNIQUE (line_item_uuid, enrollment_uuid),
    CONSTRAINT chk_course_assessment_line_item_rubric_evaluations_score CHECK (
        score IS NULL OR score >= 0.00
    ),
    CONSTRAINT chk_course_assessment_line_item_rubric_evaluations_max CHECK (
        max_score IS NULL OR max_score > 0.00
    ),
    CONSTRAINT chk_course_assessment_line_item_rubric_evaluations_percentage CHECK (
        percentage IS NULL OR
        (percentage >= 0.00 AND percentage <= 100.00)
    )
);

CREATE INDEX idx_course_assessment_line_item_rubric_evaluations_uuid
ON course_assessment_line_item_rubric_evaluations (uuid);

CREATE INDEX idx_course_assessment_line_item_rubric_evaluations_line_item_uuid
ON course_assessment_line_item_rubric_evaluations (line_item_uuid);

CREATE INDEX idx_course_assessment_line_item_rubric_evaluations_enrollment_uuid
ON course_assessment_line_item_rubric_evaluations (enrollment_uuid);

CREATE TABLE course_assessment_line_item_rubric_evaluation_rows
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    evaluation_uuid   UUID                     NOT NULL REFERENCES course_assessment_line_item_rubric_evaluations (uuid) ON DELETE CASCADE,
    criteria_uuid     UUID                     NOT NULL REFERENCES rubric_criteria (uuid),
    scoring_level_uuid UUID                    NOT NULL REFERENCES rubric_scoring_levels (uuid),
    points            DECIMAL(7, 2)            NOT NULL CHECK (points >= 0.00),
    comments          TEXT,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date      TIMESTAMP WITH TIME ZONE          DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    CONSTRAINT uq_course_assessment_line_item_rubric_evaluation_row UNIQUE (evaluation_uuid, criteria_uuid)
);

CREATE INDEX idx_course_assessment_line_item_rubric_evaluation_rows_uuid
ON course_assessment_line_item_rubric_evaluation_rows (uuid);

CREATE INDEX idx_course_assessment_line_item_rubric_evaluation_rows_evaluation_uuid
ON course_assessment_line_item_rubric_evaluation_rows (evaluation_uuid);
