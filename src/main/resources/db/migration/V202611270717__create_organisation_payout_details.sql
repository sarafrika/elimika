-- Where an organisation wants to be paid, captured during onboarding: an M-Pesa Paybill (business number and
-- account number) or Till. Typed in by an organisation admin and checked by the platform admin who verifies the
-- organisation; no code is sent, and wallet withdrawals do not use it yet. One row per organisation.
CREATE TABLE organisation_payout_details
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid UUID                     NOT NULL UNIQUE REFERENCES organisation (uuid) ON DELETE CASCADE,
    channel           VARCHAR(20)              NOT NULL,
    business_number   VARCHAR(7)               NOT NULL,
    account_number    VARCHAR(20),
    account_name      VARCHAR(120)             NOT NULL,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    CONSTRAINT chk_organisation_payout_channel CHECK (channel IN ('MPESA_PAYBILL', 'MPESA_TILL')),
    CONSTRAINT chk_organisation_payout_business_number CHECK (business_number ~ '^[0-9]{5,7}$'),
    -- A Paybill needs the account to credit; a Till has none.
    CONSTRAINT chk_organisation_payout_account_number CHECK (
        (channel = 'MPESA_PAYBILL' AND account_number IS NOT NULL AND btrim(account_number) <> '')
        OR (channel = 'MPESA_TILL' AND account_number IS NULL))
);

COMMENT ON COLUMN organisation_payout_details.business_number IS 'Paybill business number or Till (Buy Goods) number.';
COMMENT ON COLUMN organisation_payout_details.account_name IS 'Name the Paybill or Till is registered under.';
