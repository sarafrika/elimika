-- Migration to drop poc_user_uuid column from training_branches table
-- V202508261402__drop_poc_user_uuid_from_training_branches.sql

-- Drop the foreign key constraint on poc_user_uuid if it exists
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

-- Drop the poc_user_uuid column from training_branches table
ALTER TABLE training_branches DROP COLUMN IF EXISTS poc_user_uuid;