-- Creates table to persist inbound HTTP request telemetry including user metadata snapshots.

CREATE TABLE IF NOT EXISTS request_audit_log
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    request_id          VARCHAR(64) NOT NULL,
    http_method         VARCHAR(10) NOT NULL,
    request_uri         TEXT        NOT NULL,
    query_string        TEXT,
    ip_address          VARCHAR(64) NOT NULL,
    remote_host         VARCHAR(255),
    user_agent          TEXT,
    referer             TEXT,
    session_id          VARCHAR(128),
    header_snapshot     TEXT,
    response_status     INTEGER,
    processing_time_ms  BIGINT,
    authentication_name VARCHAR(255),
    user_uuid           UUID,
    user_email          VARCHAR(255),
    user_full_name      VARCHAR(255),
    user_domains        TEXT,
    keycloak_id         VARCHAR(255),
    created_date        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50) NOT NULL,
    updated_date        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50),

    CONSTRAINT fk_request_audit_user
        FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE SET NULL
);

CREATE INDEX idx_request_audit_created_date ON request_audit_log (created_date);
CREATE INDEX idx_request_audit_ip_address ON request_audit_log (ip_address);
CREATE INDEX idx_request_audit_user_uuid ON request_audit_log (user_uuid);
CREATE INDEX idx_request_audit_request_id ON request_audit_log (request_id);

COMMENT ON TABLE request_audit_log IS 'Stores HTTP request telemetry including client IP address and user metadata.';
COMMENT ON COLUMN request_audit_log.header_snapshot IS 'JSON snapshot of selected inbound headers and derived request metadata.';
