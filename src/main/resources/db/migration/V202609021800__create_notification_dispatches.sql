-- Outgoing notifications an organisation has broadcast to an audience.
--
-- One row per send. The per-recipient in-app/email notifications are created through the normal
-- notification pipeline (NotificationRequestedEvent); this table is the organisation's own record
-- of what it sent, to whom (audience), on which channel, and how many recipients it reached.
CREATE TABLE notification_dispatches
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid UUID                     NOT NULL,
    sender_user_uuid  UUID,
    audience          VARCHAR(32)              NOT NULL,
    channel           VARCHAR(16)              NOT NULL,
    title             VARCHAR(200)             NOT NULL,
    body              TEXT                     NOT NULL,
    recipient_count   INTEGER                  NOT NULL        DEFAULT 0,
    scheduled_at      TIMESTAMP WITH TIME ZONE,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_date      TIMESTAMP WITH TIME ZONE,
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_notification_dispatches_org
    ON notification_dispatches (organisation_uuid, created_date DESC);

COMMENT ON TABLE notification_dispatches IS
    'Record of an organisation-originated notification broadcast (outgoing).';
