-- V202508271500__drop_criteria_weight_and_rubric_isweighted_columns.sql
-- Remove individual criteria weighting - all criteria are now equally weighted
-- Keep rubric-level weighting for overall assessment contribution

-- Drop weight column from rubric_criteria table
ALTER TABLE rubric_criteria DROP COLUMN IF EXISTS weight;

-- Drop is_weighted column from assessment_rubrics table since criteria are always equally weighted
ALTER TABLE assessment_rubrics DROP COLUMN IF EXISTS is_weighted;

-- Add comments to clarify the new weighting approach
COMMENT ON TABLE rubric_criteria IS 'Rubric criteria with equal weighting - all criteria contribute equally to the final rubric score';
COMMENT ON COLUMN assessment_rubrics.total_weight IS 'Total weight of this rubric in the overall course assessment (rubric-level weighting)';
COMMENT ON COLUMN assessment_rubrics.weight_unit IS 'Unit for rubric weight calculation (percentage, points, ratio) - applies to entire rubric';