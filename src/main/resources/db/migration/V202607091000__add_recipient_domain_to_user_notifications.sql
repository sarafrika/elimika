-- Scope in-app notifications to the dashboard domain (role) they are addressed
-- to, so a multi-domain recipient (e.g. student + instructor) sees each
-- notification only in the relevant dashboard. NULL means "all domains".

ALTER TABLE user_notifications
    ADD COLUMN recipient_domain VARCHAR(30);

-- Backfill existing rows from the notification type's default audience,
-- mirroring NotificationType.getRecipientDomain(). Account-level types are
-- left NULL so they continue to appear in every dashboard.
UPDATE user_notifications
SET recipient_domain = CASE
    WHEN notification_type IN (
        'COURSE_ENROLLMENT_WELCOME',
        'COURSE_COMPLETION_CERTIFICATE',
        'LEARNING_MILESTONE_ACHIEVED',
        'ASSIGNMENT_DUE_REMINDER',
        'ASSIGNMENT_SUBMITTED_CONFIRMATION',
        'ASSIGNMENT_GRADED',
        'ASSIGNMENT_RETURNED_FOR_REVISION',
        'ASSIGNMENT_DEADLINE_REMINDER',
        'ASSESSMENT_COMPLETED',
        'CLASS_ENROLLMENT_CONFIRMED',
        'UPCOMING_CLASS_REMINDER',
        'LEARNING_CERTIFICATE_ISSUED',
        'WEEKLY_PROGRESS_SUMMARY',
        'LEARNING_STREAK_ACHIEVEMENT',
        'PEER_ACHIEVEMENT_CELEBRATION'
    ) THEN 'student'
    WHEN notification_type IN (
        'NEW_STUDENT_ENROLLMENT',
        'NEW_ASSIGNMENT_SUBMISSION',
        'CLASS_SCHEDULE_UPDATED',
        'GRADING_REMINDER',
        'INSTRUCTOR_CLASS_ENROLLMENT_MILESTONE',
        'INSTRUCTOR_CLASS_ENROLLMENT_NOTICE',
        'COURSE_TRAINING_APPLICATION_APPROVED',
        'COURSE_TRAINING_APPLICATION_REJECTED',
        'COURSE_TRAINING_APPLICATION_REVOKED',
        'PROGRAM_TRAINING_APPLICATION_APPROVED',
        'PROGRAM_TRAINING_APPLICATION_REJECTED',
        'PROGRAM_TRAINING_APPLICATION_REVOKED'
    ) THEN 'instructor'
    WHEN notification_type IN (
        'COURSE_CONTENT_APPROVED',
        'COURSE_CONTENT_REJECTED',
        'PROGRAM_CONTENT_APPROVED',
        'PROGRAM_CONTENT_REJECTED',
        'COURSE_TRAINING_APPLICATION_SUBMITTED',
        'PROGRAM_TRAINING_APPLICATION_SUBMITTED',
        'COURSE_ENROLLMENT_MILESTONE',
        'COURSE_ENROLLMENT_NOTICE'
    ) THEN 'course_creator'
    ELSE NULL
END;

CREATE INDEX idx_user_notifications_recipient_domain
    ON user_notifications (recipient_uuid, recipient_domain, status);
