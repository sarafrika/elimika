-- The M-Pesa number an organisation wants to be paid on, captured during onboarding. Typed in by an organisation
-- admin and checked by the platform admin who verifies the organisation; no code is sent, and wallet withdrawals
-- do not use it yet. One row per organisation.
CREATE TABLE organisation_payout_details
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid UUID                     NOT NULL UNIQUE REFERENCES organisation (uuid) ON DELETE CASCADE,
    mpesa_number      VARCHAR(12)              NOT NULL,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    CONSTRAINT chk_organisation_payout_mpesa_number CHECK (mpesa_number ~ '^254[17][0-9]{8}$')
);

COMMENT ON COLUMN organisation_payout_details.mpesa_number IS 'Kenyan M-Pesa number, stored as 2547XXXXXXXX or 2541XXXXXXXX.';
