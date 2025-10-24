-- 202511071200__Add_Course_Training_Requirements_and_Class_Pricing.sql
-- Introduce minimum training fees, delivery requirements, and revenue share metadata

-- Add minimum training fee to courses
ALTER TABLE courses
    ADD COLUMN minimum_training_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE courses
    ADD CONSTRAINT chk_courses_minimum_training_fee_non_negative
        CHECK (minimum_training_fee >= 0);

-- Create course training requirements table for delivery resources (materials, equipment, etc.)
CREATE TABLE course_training_requirements
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid      UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    requirement_type VARCHAR(20)              NOT NULL CHECK (requirement_type IN ('material', 'equipment', 'facility', 'other')),
    name             VARCHAR(255)             NOT NULL,
    description      TEXT,
    quantity         INTEGER,
    unit             VARCHAR(50),
    provided_by      VARCHAR(30)                          CHECK (provided_by IN ('course_creator', 'instructor', 'organisation', 'student')),
    is_mandatory     BOOLEAN                  NOT NULL DEFAULT true,
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE          DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255)
);

CREATE INDEX idx_course_training_requirements_uuid
    ON course_training_requirements (uuid);

CREATE INDEX idx_course_training_requirements_course_uuid
    ON course_training_requirements (course_uuid);

CREATE INDEX idx_course_training_requirements_type
    ON course_training_requirements (requirement_type);

-- Extend class definitions with pricing metadata
ALTER TABLE class_definitions
    ADD COLUMN training_fee NUMERIC(12, 2);

ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_training_fee_non_negative
        CHECK (training_fee IS NULL OR training_fee >= 0);

-- Add revenue share metadata to courses
ALTER TABLE courses
    ADD COLUMN creator_share_percentage NUMERIC(5, 2),
    ADD COLUMN instructor_share_percentage NUMERIC(5, 2),
    ADD COLUMN revenue_share_notes TEXT;

ALTER TABLE courses
    ADD CONSTRAINT chk_courses_revenue_share_sum
        CHECK (
                (creator_share_percentage IS NULL AND instructor_share_percentage IS NULL)
                OR (
                        creator_share_percentage >= 0
                    AND instructor_share_percentage >= 0
                    AND creator_share_percentage + instructor_share_percentage = 100.00
                )
        );
