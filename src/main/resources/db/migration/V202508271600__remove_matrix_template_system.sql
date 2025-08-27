-- V202508271600__remove_matrix_template_system.sql
-- Remove the matrix template system and associated database triggers
-- This migration supports the transition to user-defined custom scoring levels

-- Drop the trigger first
DROP TRIGGER IF EXISTS tr_create_default_scoring_levels ON assessment_rubrics;

-- Drop the trigger function
DROP FUNCTION IF EXISTS create_default_scoring_levels_for_rubric();

-- Remove the matrix_template column from assessment_rubrics table
-- This column is no longer needed as users will define their own custom scoring levels
ALTER TABLE assessment_rubrics 
DROP COLUMN IF EXISTS matrix_template;

-- Drop the index that was created for matrix_template
DROP INDEX IF EXISTS idx_assessment_rubrics_matrix_template;

-- Update any existing rubrics to use custom levels if they have scoring levels defined
-- This ensures compatibility with existing data
DO $$
DECLARE
    rubric_record RECORD;
    level_count INTEGER;
BEGIN
    -- Loop through rubrics that might have template-generated levels
    FOR rubric_record IN 
        SELECT uuid, uses_custom_levels
        FROM assessment_rubrics 
        WHERE uses_custom_levels = false
    LOOP
        -- Check if this rubric has any custom scoring levels
        SELECT COUNT(*) INTO level_count
        FROM rubric_scoring_levels 
        WHERE rubric_uuid = rubric_record.uuid;
        
        -- If it has scoring levels, mark it as using custom levels
        IF level_count > 0 THEN
            UPDATE assessment_rubrics 
            SET uses_custom_levels = true
            WHERE uuid = rubric_record.uuid;
            
            RAISE NOTICE 'Updated rubric % to use custom levels (% levels found)', 
                rubric_record.uuid, level_count;
        END IF;
    END LOOP;
END $$;

-- Add helpful comment about the change
COMMENT ON COLUMN assessment_rubrics.uses_custom_levels IS 
    'Whether this rubric uses custom scoring levels. All rubrics now use custom levels defined by users.';

-- Log the migration completion
DO $$
BEGIN
    RAISE NOTICE 'Matrix template system successfully removed. All rubrics now use user-defined custom scoring levels.';
END $$;