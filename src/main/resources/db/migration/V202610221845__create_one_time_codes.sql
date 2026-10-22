-- Six-digit codes sent by SMS. Only an HMAC of each code is kept; a code is bound to one purpose and
-- one subject (a payout method, a withdrawal), expires, allows a few tries and works once.
CREATE TABLE one_time_codes
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    purpose             VARCHAR(40)  NOT NULL,
    subject_uuid        UUID         NOT NULL,
    requested_by        UUID,
    code_hash           VARCHAR(64)  NOT NULL,
    sent_to             VARCHAR(32)  NOT NULL,
    provider_message_id VARCHAR(64),
    expires_at          TIMESTAMPTZ  NOT NULL,
    attempts            INTEGER      NOT NULL DEFAULT 0,
    max_attempts        INTEGER      NOT NULL,
    consumed_at         TIMESTAMPTZ,
    invalidated_at      TIMESTAMPTZ,
    created_date        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50)  NOT NULL,
    updated_date        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(50),
    CONSTRAINT chk_one_time_code_purpose CHECK (purpose IN ('PAYOUT_METHOD_VERIFICATION', 'WITHDRAWAL_CONFIRMATION')),
    CONSTRAINT chk_one_time_code_attempts CHECK (attempts >= 0 AND attempts <= max_attempts)
);

CREATE INDEX idx_one_time_codes_subject ON one_time_codes (purpose, subject_uuid, id DESC);
