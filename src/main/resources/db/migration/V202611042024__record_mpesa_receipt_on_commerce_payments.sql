-- The M-Pesa receipt (e.g. UK42C120ZR) and the number that paid, so receipts by SMS and email
-- can quote the receipt and reach the phone the money actually came from.
ALTER TABLE commerce_payment ADD COLUMN IF NOT EXISTS provider_receipt VARCHAR(32);
ALTER TABLE commerce_payment ADD COLUMN IF NOT EXISTS payer_msisdn VARCHAR(16);

COMMENT ON COLUMN commerce_payment.provider_receipt IS 'Gateway receipt number, e.g. the M-Pesa transaction id';
COMMENT ON COLUMN commerce_payment.payer_msisdn IS 'Normalised 2547XXXXXXXX number that paid; never shown unmasked';
