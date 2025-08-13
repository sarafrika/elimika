-- V202508131100__add_rubric_scoring_levels.sql
-- Add support for custom scoring levels per rubric to enable flexible matrix configurations

-- Create rubric scoring levels table for custom level definitions per rubric
CREATE TABLE rubric_scoring_levels
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    rubric_uuid  UUID                     NOT NULL REFERENCES assessment_rubrics (uuid) ON DELETE CASCADE,
    name         VARCHAR(50)              NOT NULL,
    description  TEXT,
    points       DECIMAL(5, 2)            NOT NULL DEFAULT 0.00,
    level_order  INTEGER                  NOT NULL,
    color_code   VARCHAR(7),                                      -- Optional hex color for UI (#FF5733)
    is_passing   BOOLEAN                  NOT NULL DEFAULT false, -- Indicates if this level constitutes a pass
    created_date TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by   VARCHAR(255)             NOT NULL,
    updated_by   VARCHAR(255),
    
    -- Ensure unique combination of rubric and level order
    UNIQUE (rubric_uuid, level_order),
    -- Ensure unique level names within a rubric
    UNIQUE (rubric_uuid, name),
    -- Ensure valid ordering
    CHECK (level_order > 0),
    -- Ensure valid points
    CHECK (points >= 0.00 AND points <= 1000.00),
    -- Ensure valid color code format if provided
    CHECK (color_code IS NULL OR color_code ~ '^#[0-9A-Fa-f]{6}$')
);

-- Add reference to rubric scoring levels in rubric_scoring table
ALTER TABLE rubric_scoring 
ADD COLUMN rubric_scoring_level_uuid UUID REFERENCES rubric_scoring_levels (uuid) ON DELETE CASCADE;

-- Add index to determine if we're using global or rubric-specific levels
CREATE INDEX idx_rubric_scoring_level_type ON rubric_scoring (grading_level_uuid, rubric_scoring_level_uuid);

-- Create performance indexes for rubric scoring levels
CREATE INDEX idx_rubric_scoring_levels_uuid ON rubric_scoring_levels (uuid);
CREATE INDEX idx_rubric_scoring_levels_rubric_uuid ON rubric_scoring_levels (rubric_uuid);
CREATE INDEX idx_rubric_scoring_levels_order ON rubric_scoring_levels (rubric_uuid, level_order);
CREATE INDEX idx_rubric_scoring_levels_name ON rubric_scoring_levels (rubric_uuid, name);
CREATE INDEX idx_rubric_scoring_levels_points ON rubric_scoring_levels (points);
CREATE INDEX idx_rubric_scoring_levels_created_date ON rubric_scoring_levels (created_date);

-- Add constraint to rubric_scoring to use either global or rubric-specific levels, but not both
ALTER TABLE rubric_scoring
ADD CONSTRAINT chk_scoring_level_reference 
CHECK (
    (grading_level_uuid IS NOT NULL AND rubric_scoring_level_uuid IS NULL) OR
    (grading_level_uuid IS NULL AND rubric_scoring_level_uuid IS NOT NULL)
);

-- Add columns to assessment_rubrics to support matrix configuration
ALTER TABLE assessment_rubrics
ADD COLUMN uses_custom_levels BOOLEAN DEFAULT false NOT NULL,
ADD COLUMN matrix_template VARCHAR(50) DEFAULT 'standard',
ADD COLUMN max_score DECIMAL(5, 2),
ADD COLUMN min_passing_score DECIMAL(5, 2);

-- Add constraints for score validation
ALTER TABLE assessment_rubrics
ADD CONSTRAINT chk_max_score_positive 
CHECK (max_score IS NULL OR max_score > 0.00);

ALTER TABLE assessment_rubrics
ADD CONSTRAINT chk_min_passing_score_valid 
CHECK (min_passing_score IS NULL OR (min_passing_score >= 0.00 AND (max_score IS NULL OR min_passing_score <= max_score)));

-- Create indexes for the new assessment_rubrics columns
CREATE INDEX idx_assessment_rubrics_uses_custom_levels ON assessment_rubrics (uses_custom_levels);
CREATE INDEX idx_assessment_rubrics_matrix_template ON assessment_rubrics (matrix_template);

-- Create function to automatically set default scoring levels for new rubrics
CREATE OR REPLACE FUNCTION create_default_scoring_levels_for_rubric()
RETURNS TRIGGER AS $$
BEGIN
    -- Only create default levels if this is a new rubric using custom levels
    IF NEW.uses_custom_levels = true AND OLD.uses_custom_levels IS DISTINCT FROM NEW.uses_custom_levels THEN
        -- Insert default scoring levels based on matrix template
        CASE NEW.matrix_template
            WHEN 'standard' THEN
                INSERT INTO rubric_scoring_levels (rubric_uuid, name, description, points, level_order, color_code, is_passing, created_by)
                VALUES 
                    (NEW.uuid, 'Excellent', 'Exceeds expectations in all areas', 4.00, 1, '#4CAF50', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Good', 'Meets expectations with minor areas for improvement', 3.00, 2, '#8BC34A', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Fair', 'Meets basic expectations with several areas needing improvement', 2.00, 3, '#FFC107', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Poor', 'Below expectations, significant improvement needed', 1.00, 4, '#FF9800', false, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Unacceptable', 'Does not meet minimum requirements', 0.00, 5, '#F44336', false, COALESCE(NEW.created_by, 'SYSTEM'));
            WHEN 'simple' THEN
                INSERT INTO rubric_scoring_levels (rubric_uuid, name, description, points, level_order, color_code, is_passing, created_by)
                VALUES 
                    (NEW.uuid, 'Proficient', 'Demonstrates proficiency', 3.00, 1, '#4CAF50', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Developing', 'Shows progress toward proficiency', 2.00, 2, '#FFC107', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Beginning', 'Limited evidence of understanding', 1.00, 3, '#F44336', false, COALESCE(NEW.created_by, 'SYSTEM'));
            WHEN 'advanced' THEN
                INSERT INTO rubric_scoring_levels (rubric_uuid, name, description, points, level_order, color_code, is_passing, created_by)
                VALUES 
                    (NEW.uuid, 'Exemplary', 'Exceptional performance', 5.00, 1, '#2E7D32', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Proficient', 'Solid performance', 4.00, 2, '#4CAF50', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Developing', 'Adequate performance', 3.00, 3, '#8BC34A', true, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Beginning', 'Needs improvement', 2.00, 4, '#FFC107', false, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'Inadequate', 'Significant deficiencies', 1.00, 5, '#FF5722', false, COALESCE(NEW.created_by, 'SYSTEM')),
                    (NEW.uuid, 'No Evidence', 'No evidence of understanding', 0.00, 6, '#F44336', false, COALESCE(NEW.created_by, 'SYSTEM'));
        END CASE;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically create default scoring levels
CREATE TRIGGER tr_create_default_scoring_levels
    AFTER INSERT OR UPDATE OF uses_custom_levels, matrix_template ON assessment_rubrics
    FOR EACH ROW
    EXECUTE FUNCTION create_default_scoring_levels_for_rubric();

-- Add helpful comments to new columns and tables
COMMENT ON TABLE rubric_scoring_levels IS 'Custom scoring levels defined per rubric for flexible matrix configurations';
COMMENT ON COLUMN rubric_scoring_levels.name IS 'Display name for the scoring level (e.g., Excellent, Good, Fair)';
COMMENT ON COLUMN rubric_scoring_levels.points IS 'Point value assigned to this scoring level';
COMMENT ON COLUMN rubric_scoring_levels.level_order IS 'Display order of the level in the rubric matrix (1 = highest)';
COMMENT ON COLUMN rubric_scoring_levels.color_code IS 'Optional hex color code for UI display (#RRGGBB format)';
COMMENT ON COLUMN rubric_scoring_levels.is_passing IS 'Whether this scoring level constitutes a passing grade';

COMMENT ON COLUMN assessment_rubrics.uses_custom_levels IS 'Whether this rubric uses custom scoring levels or global grading levels';
COMMENT ON COLUMN assessment_rubrics.matrix_template IS 'Template used for default scoring levels (standard, simple, advanced)';
COMMENT ON COLUMN assessment_rubrics.max_score IS 'Maximum possible score for this rubric based on weighted criteria and levels';
COMMENT ON COLUMN assessment_rubrics.min_passing_score IS 'Minimum score required to pass this rubric assessment';

COMMENT ON COLUMN rubric_scoring.rubric_scoring_level_uuid IS 'Reference to rubric-specific scoring level (alternative to grading_level_uuid)';