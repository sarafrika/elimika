-- Hiring block on the Post a job form: free-text requirements and who may be hired
-- (any instructor, or only the organisation's own). NULL reads as ANY.
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS requirements VARCHAR(500);
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS membership_preference VARCHAR(30);
ALTER TABLE class_marketplace_jobs DROP CONSTRAINT IF EXISTS chk_class_marketplace_jobs_membership_preference;
ALTER TABLE class_marketplace_jobs ADD CONSTRAINT chk_class_marketplace_jobs_membership_preference
    CHECK (membership_preference IS NULL OR membership_preference IN ('ANY', 'ORGANISATION_ONLY'));
