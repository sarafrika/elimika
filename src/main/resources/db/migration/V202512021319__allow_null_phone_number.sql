-- Allows phone_number on users to be nullable to support optional phone capture.

ALTER TABLE users
    ALTER COLUMN phone_number DROP NOT NULL;
