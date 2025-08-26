-- Remove organisation code generation procedures and sequences

-- 1. Drop the trigger first
DROP TRIGGER IF EXISTS set_organisation_code_before_insert ON organisation;

-- 2. Drop the trigger function
DROP FUNCTION IF EXISTS set_organisation_code();

-- 3. Drop the code generation function
DROP FUNCTION IF EXISTS generate_unique_organisation_code();

-- 4. Drop the sequence
DROP SEQUENCE IF EXISTS organisation_code_seq;