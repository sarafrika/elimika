-- Real-user performance samples sent by the web app; rows older than 30 days are purged by a job.

CREATE TABLE perf_rum_event
(
    id             BIGSERIAL PRIMARY KEY,
    route_template VARCHAR(255)     NOT NULL,
    domain         VARCHAR(64),
    metric         VARCHAR(64)      NOT NULL,
    value_ms       DOUBLE PRECISION NOT NULL,
    section        VARCHAR(128),
    network_type   VARCHAR(32),
    device_class   VARCHAR(32),
    app_version    VARCHAR(64),
    authenticated  BOOLEAN          NOT NULL DEFAULT FALSE,
    occurred_at    TIMESTAMPTZ      NOT NULL,
    received_at    TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_perf_rum_event_value_ms CHECK (value_ms >= 0)
);

CREATE INDEX idx_perf_rum_event_occurred_at ON perf_rum_event (occurred_at);

CREATE INDEX idx_perf_rum_event_route_template ON perf_rum_event (route_template, metric, occurred_at);
