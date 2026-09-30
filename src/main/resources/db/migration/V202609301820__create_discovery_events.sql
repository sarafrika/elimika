-- Discovery tracking: which recommended items a user was shown (impressions, recorded server-side by
-- recommenders) and what they did with them (clicks and dismissals, posted by the client).
-- Rows are purged after 180 days. No query text is ever stored here.

CREATE TABLE IF NOT EXISTS discovery_events
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_uuid         UUID        NOT NULL,
    surface           VARCHAR(64) NOT NULL,
    recommendation_id UUID        NOT NULL,
    item_type         VARCHAR(64) NOT NULL,
    item_uuid         UUID        NOT NULL,
    position          INTEGER     NOT NULL,
    event_type        VARCHAR(16) NOT NULL,
    reason_codes      TEXT[]      NOT NULL DEFAULT '{}',
    model_version     VARCHAR(64),
    created_at        TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_date      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(255) NOT NULL,
    updated_date      TIMESTAMP,
    updated_by        VARCHAR(255),

    CONSTRAINT chk_discovery_events_event_type CHECK (event_type IN ('IMPRESSION', 'CLICK', 'DISMISS')),
    CONSTRAINT chk_discovery_events_position CHECK (position >= 0),
    CONSTRAINT fk_discovery_events_user
        FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_discovery_events_created_at ON discovery_events (created_at);
CREATE INDEX idx_discovery_events_recommendation ON discovery_events (recommendation_id, item_uuid);
CREATE INDEX idx_discovery_events_user_created ON discovery_events (user_uuid, created_at);

COMMENT ON TABLE discovery_events IS 'Impressions, clicks and dismissals of recommended items; kept 180 days. Never holds query text.';
COMMENT ON COLUMN discovery_events.recommendation_id IS 'Identifies one recommendation response; clicks and dismissals join back to its impressions.';
COMMENT ON COLUMN discovery_events.reason_codes IS 'Machine-readable reasons the item was recommended, copied from the impression.';
