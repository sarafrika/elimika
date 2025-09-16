-- Add admin verification flag to organisation table
-- This allows admins to verify/approve organizations similar to instructor verification

ALTER TABLE organisation
    ADD COLUMN admin_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for efficient filtering of verified organizations
CREATE INDEX idx_organisation_admin_verified ON organisation (admin_verified);

-- Add comment for clarity
COMMENT ON COLUMN organisation.admin_verified IS 'Flag indicating whether the organisation has been verified/approved by an admin';