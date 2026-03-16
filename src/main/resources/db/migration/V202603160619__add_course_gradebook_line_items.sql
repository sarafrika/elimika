-- Add weighted gradebook support on top of course assessment components.

ALTER TABLE course_assessments
ADD COLUMN aggregation_strategy VARCHAR(30) NOT NULL DEFAULT 'points_sum'
CHECK (aggregation_strategy IN ('points_sum', 'weighted_average'));

CREATE TABLE course_assessment_line_items
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_assessment_uuid UUID                     NOT NULL REFERENCES course_assessments (uuid) ON DELETE CASCADE,
    title                  VARCHAR(255)             NOT NULL,
    description            TEXT,
    item_type              VARCHAR(30)              NOT NULL CHECK (item_type IN (
        'assignment',
        'quiz',
        'attendance',
        'project',
        'discussion',
        'exam',
        'practical',
        'performance',
        'participation',
        'manual'
    )),
    assignment_uuid        UUID REFERENCES assignments (uuid),
    quiz_uuid              UUID REFERENCES quizzes (uuid),
    rubric_uuid            UUID REFERENCES assessment_rubrics (uuid),
    max_score              DECIMAL(7, 2),
    weight_percentage      DECIMAL(5, 2),
    display_order          INTEGER                  NOT NULL DEFAULT 1 CHECK (display_order > 0),
    active                 BOOLEAN                  NOT NULL DEFAULT true,
    due_at                 TIMESTAMP WITH TIME ZONE,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date           TIMESTAMP WITH TIME ZONE          DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by             VARCHAR(255)             NOT NULL,
    updated_by             VARCHAR(255),
    CONSTRAINT chk_course_assessment_line_items_reference CHECK (
        (assignment_uuid IS NOT NULL AND quiz_uuid IS NULL) OR
        (assignment_uuid IS NULL AND quiz_uuid IS NOT NULL) OR
        (assignment_uuid IS NULL AND quiz_uuid IS NULL)
    ),
    CONSTRAINT chk_course_assessment_line_items_max_score CHECK (max_score IS NULL OR max_score > 0.00),
    CONSTRAINT chk_course_assessment_line_items_weight CHECK (
        weight_percentage IS NULL OR
        (weight_percentage > 0.00 AND weight_percentage <= 100.00)
    )
);

CREATE UNIQUE INDEX uq_course_assessment_line_items_assignment_uuid
ON course_assessment_line_items (assignment_uuid)
WHERE assignment_uuid IS NOT NULL;

CREATE UNIQUE INDEX uq_course_assessment_line_items_quiz_uuid
ON course_assessment_line_items (quiz_uuid)
WHERE quiz_uuid IS NOT NULL;

CREATE INDEX idx_course_assessment_line_items_uuid ON course_assessment_line_items (uuid);
CREATE INDEX idx_course_assessment_line_items_assessment_uuid ON course_assessment_line_items (course_assessment_uuid);
CREATE INDEX idx_course_assessment_line_items_display_order ON course_assessment_line_items (course_assessment_uuid, display_order);

CREATE TABLE course_assessment_line_item_scores
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    line_item_uuid  UUID                     NOT NULL REFERENCES course_assessment_line_items (uuid) ON DELETE CASCADE,
    enrollment_uuid UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    score           DECIMAL(7, 2),
    max_score       DECIMAL(7, 2),
    percentage      DECIMAL(5, 2),
    comments        TEXT,
    graded_at       TIMESTAMP WITH TIME ZONE,
    graded_by_uuid  UUID REFERENCES users (uuid),
    created_date    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date    TIMESTAMP WITH TIME ZONE          DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by      VARCHAR(255)             NOT NULL,
    updated_by      VARCHAR(255),
    CONSTRAINT uq_course_assessment_line_item_score UNIQUE (line_item_uuid, enrollment_uuid),
    CONSTRAINT chk_course_assessment_line_item_scores_score CHECK (
        score IS NULL OR score >= 0.00
    ),
    CONSTRAINT chk_course_assessment_line_item_scores_max CHECK (
        max_score IS NULL OR max_score > 0.00
    ),
    CONSTRAINT chk_course_assessment_line_item_scores_percentage CHECK (
        percentage IS NULL OR
        (percentage >= 0.00 AND percentage <= 100.00)
    )
);

CREATE INDEX idx_course_assessment_line_item_scores_uuid ON course_assessment_line_item_scores (uuid);
CREATE INDEX idx_course_assessment_line_item_scores_line_item_uuid ON course_assessment_line_item_scores (line_item_uuid);
CREATE INDEX idx_course_assessment_line_item_scores_enrollment_uuid ON course_assessment_line_item_scores (enrollment_uuid);
