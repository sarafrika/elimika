-- Additional class-advert attributes surfaced by the organisation "Create a class"
-- builder (Lovable design): the chosen service type, a preferred instructor hint,
-- target learner groups, and reminder recipients/channels. All nullable — existing
-- rows keep NULL and behave exactly as before.
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS service_type VARCHAR(30);
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS preferred_instructor_uuid UUID;
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS target_groups TEXT;
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS remind_students BOOLEAN;
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS remind_instructor BOOLEAN;
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS remind_via_email BOOLEAN;
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS remind_via_sms BOOLEAN;
ALTER TABLE class_marketplace_jobs ADD COLUMN IF NOT EXISTS remind_via_push BOOLEAN;
