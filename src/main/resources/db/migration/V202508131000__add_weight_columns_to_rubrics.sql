-- V202508131000__add_weight_columns_to_rubrics.sql
-- Add weight support to assessment rubrics and criteria for weighted evaluation

-- Add total_weight column to assessment_rubrics table
-- This represents the total weight distribution across all criteria (typically 100.00)
ALTER TABLE assessment_rubrics
ADD COLUMN total_weight DECIMAL(5,2) DEFAULT 100.00 NOT NULL
CHECK (total_weight > 0 AND total_weight <= 1000.00);

-- Add weight column to rubric_criteria table
-- This represents the individual weight/percentage for each criteria
ALTER TABLE rubric_criteria
ADD COLUMN weight DECIMAL(5,2) DEFAULT 0.00 NOT NULL
CHECK (weight >= 0 AND weight <= 100.00);

-- Add weight_unit column to assessment_rubrics table to support different weighting systems
-- Values: 'percentage' (0-100), 'points' (custom total), 'ratio' (0-1)
ALTER TABLE assessment_rubrics
ADD COLUMN weight_unit VARCHAR(20) DEFAULT 'percentage' NOT NULL
CHECK (weight_unit IN ('percentage', 'points', 'ratio'));

-- Add is_weighted column to assessment_rubrics table
-- Indicates whether this rubric uses weighted evaluation or equal distribution
ALTER TABLE assessment_rubrics
ADD COLUMN is_weighted BOOLEAN DEFAULT true NOT NULL;

-- Create index for performance optimization on weight queries
CREATE INDEX idx_assessment_rubrics_is_weighted ON assessment_rubrics (is_weighted);
CREATE INDEX idx_assessment_rubrics_total_weight ON assessment_rubrics (total_weight);
CREATE INDEX idx_rubric_criteria_weight ON rubric_criteria (weight);

-- Add constraint to ensure reasonable weight distribution
-- This will be enforced at application level for more complex validation
ALTER TABLE rubric_criteria
ADD CONSTRAINT chk_criteria_weight_range 
CHECK (weight >= 0.00 AND weight <= 100.00);

-- Update existing records to have default weights
-- For existing rubrics without weights, distribute equally among criteria
DO $$
DECLARE
    rubric_record RECORD;
    criteria_count INTEGER;
    equal_weight DECIMAL(5,2);
BEGIN
    -- Loop through each rubric and set equal weights for existing criteria
    FOR rubric_record IN 
        SELECT DISTINCT rubric_uuid 
        FROM rubric_criteria 
        WHERE weight = 0.00
    LOOP
        -- Count criteria for this rubric
        SELECT COUNT(*) INTO criteria_count
        FROM rubric_criteria 
        WHERE rubric_uuid = rubric_record.rubric_uuid;
        
        -- Calculate equal distribution weight
        IF criteria_count > 0 THEN
            equal_weight := ROUND(100.00 / criteria_count, 2);
            
            -- Update all criteria for this rubric with equal weight
            UPDATE rubric_criteria 
            SET weight = equal_weight
            WHERE rubric_uuid = rubric_record.rubric_uuid;
            
            -- Log the update for audit purposes
            RAISE NOTICE 'Updated rubric % with % criteria, each weighted at %', 
                rubric_record.rubric_uuid, criteria_count, equal_weight;
        END IF;
    END LOOP;
END $$;

-- Add helpful comments to columns
COMMENT ON COLUMN assessment_rubrics.total_weight IS 'Total weight distribution across all criteria (typically 100.00 for percentage-based)';
COMMENT ON COLUMN assessment_rubrics.weight_unit IS 'Unit of measurement for weights: percentage, points, or ratio';
COMMENT ON COLUMN assessment_rubrics.is_weighted IS 'Whether this rubric uses weighted evaluation or equal distribution';
COMMENT ON COLUMN rubric_criteria.weight IS 'Individual weight/percentage for this criteria (0-100 for percentage-based)';