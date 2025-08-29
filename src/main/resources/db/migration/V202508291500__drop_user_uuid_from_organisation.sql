-- Drop user_uuid column from organisation table
-- This field violates proper database normalization and separation of concerns
-- User-organisation relationships should be managed through user_organisation_domain_mapping table

ALTER TABLE organisation DROP COLUMN IF EXISTS user_uuid;