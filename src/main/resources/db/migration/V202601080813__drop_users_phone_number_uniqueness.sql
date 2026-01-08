-- Allow duplicate phone_number values for users.
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_phone_number_key;
