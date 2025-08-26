-- Drop keycloak_id column from organisation table as Keycloak integration is being removed
ALTER TABLE organisation DROP COLUMN IF EXISTS keycloak_id;