-- V202508281000__decouple_rubric_scoring_from_grading_levels.sql
-- This migration completely removes the old grading levels system for rubrics in favor of the new
-- rubric-specific scoring levels. The grading_levels table is kept for other purposes.

-- Step 1: Drop the check constraint that enforces mutual exclusivity between the two level types.
ALTER TABLE rubric_scoring
DROP CONSTRAINT IF EXISTS chk_scoring_level_reference;

-- Step 2: Drop the foreign key constraint from rubric_scoring to grading_levels.
-- The constraint name is 'rubric_scoring_grading_level_uuid_fkey' from the error.
ALTER TABLE rubric_scoring
DROP CONSTRAINT IF EXISTS rubric_scoring_grading_level_uuid_fkey;

-- Step 3: Drop the now-unused grading_level_uuid column from the rubric_scoring table.
ALTER TABLE rubric_scoring
DROP COLUMN IF EXISTS grading_level_uuid;

-- Step 4: Make the rubric_scoring_level_uuid column non-nullable, as it's now the only option.
ALTER TABLE rubric_scoring
ALTER COLUMN rubric_scoring_level_uuid SET NOT NULL;

-- Step 5: Drop the unique constraint that involved grading_level_uuid
ALTER TABLE rubric_scoring
DROP CONSTRAINT IF EXISTS rubric_scoring_criteria_uuid_grading_level_uuid_key;

-- Add a new unique constraint on criteria_uuid and rubric_scoring_level_uuid
ALTER TABLE rubric_scoring
ADD CONSTRAINT rubric_scoring_criteria_uuid_rubric_scoring_level_uuid_key UNIQUE (criteria_uuid, rubric_scoring_level_uuid);
