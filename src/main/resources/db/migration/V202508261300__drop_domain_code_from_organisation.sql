-- Drop domain and code columns from organisation table

-- Drop the domain column
ALTER TABLE organisation DROP COLUMN IF EXISTS domain;

-- Drop the code column
ALTER TABLE organisation DROP COLUMN IF EXISTS code;