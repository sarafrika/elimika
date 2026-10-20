-- Reminder schedulers run every minute; these record the start/due time a reminder was already sent for,
-- so each reminder is published once (and again only if the session or deadline moves).
ALTER TABLE scheduled_instances
    ADD COLUMN reminder_sent_for TIMESTAMPTZ;

ALTER TABLE class_assignment_schedules
    ADD COLUMN one_day_reminder_sent_for TIMESTAMPTZ,
    ADD COLUMN due_reminder_sent_for TIMESTAMPTZ;
