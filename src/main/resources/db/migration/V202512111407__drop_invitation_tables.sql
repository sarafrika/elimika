-- Removes legacy invitation-related tables and data. No backward compatibility is retained.

DROP TABLE IF EXISTS bulk_invitation_uploads CASCADE;
DROP TABLE IF EXISTS invitations CASCADE;
