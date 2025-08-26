-- Migration to add point of contact fields to training_branches table
-- V202508261400__add_poc_fields_to_training_branches.sql

-- Add point of contact fields to training_branches table
ALTER TABLE training_branches
    ADD COLUMN IF NOT EXISTS poc_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS poc_email VARCHAR(320),
    ADD COLUMN IF NOT EXISTS poc_telephone VARCHAR(20);

-- Create index for poc_email for faster lookups
CREATE INDEX IF NOT EXISTS idx_training_branches_poc_email ON training_branches (poc_email);

-- Drop the foreign key constraint on poc_user_uuid if it exists
-- Note: The column will be kept for backward compatibility but constraint removed
DO $$
BEGIN
    -- Check if constraint exists and drop it
    IF EXISTS (
        SELECT 1 
        FROM information_schema.table_constraints 
        WHERE constraint_name LIKE '%poc_user_uuid%' 
        AND table_name = 'training_branches'
        AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE training_branches DROP CONSTRAINT IF EXISTS training_branches_poc_user_uuid_fkey;
    END IF;
END $$;