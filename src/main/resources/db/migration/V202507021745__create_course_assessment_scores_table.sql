-- 202507021745__create_course_assessment_scores_table.sql
-- Create course assessment scores table for storing evaluation results

CREATE TABLE course_assessment_scores
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    enrollment_uuid UUID                     NOT NULL REFERENCES course_enrollments (uuid) ON DELETE CASCADE,
    assessment_uuid UUID                     NOT NULL REFERENCES course_assessments (uuid),
    score           DECIMAL(5, 2),
    max_score       DECIMAL(5, 2),
    percentage      DECIMAL(5, 2),
    graded_at       TIMESTAMP WITH TIME ZONE,
    graded_by_uuid  UUID REFERENCES users (uuid),
    comments        TEXT,
    created_date    TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date    TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by      VARCHAR(255)             NOT NULL,
    updated_by      VARCHAR(255),
    UNIQUE (enrollment_uuid, assessment_uuid)
);

-- Create performance indexes
CREATE INDEX idx_course_assessment_scores_uuid ON course_assessment_scores (uuid);
CREATE INDEX idx_course_assessment_scores_enrollment_uuid ON course_assessment_scores (enrollment_uuid);
CREATE INDEX idx_course_assessment_scores_assessment_uuid ON course_assessment_scores (assessment_uuid);
CREATE INDEX idx_course_assessment_scores_graded_by_uuid ON course_assessment_scores (graded_by_uuid);
CREATE INDEX idx_course_assessment_scores_created_date ON course_assessment_scores (created_date);