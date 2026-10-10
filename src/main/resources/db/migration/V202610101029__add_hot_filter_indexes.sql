-- Indexes for hot read paths; schema only, no rows are touched.

-- Class timetables filter by class and start-time window, then order by start time.
CREATE INDEX IF NOT EXISTS idx_scheduled_instances_definition_start
    ON scheduled_instances (class_definition_uuid, start_time);

-- The composite above has class_definition_uuid as its prefix, so the single-column index is redundant.
DROP INDEX IF EXISTS idx_scheduled_instances_definition_uuid;

-- The popup poll reads a recipient's unseen popups newest first.
CREATE INDEX IF NOT EXISTS idx_user_notifications_unseen_popups
    ON user_notifications (recipient_uuid, created_date DESC)
    WHERE presentation = 'POPUP' AND popup_seen_at IS NULL;

-- Course access checks look up approved applications by applicant without the applicant type.
CREATE INDEX IF NOT EXISTS idx_course_training_applications_applicant_status
    ON course_training_applications (applicant_uuid, status);
