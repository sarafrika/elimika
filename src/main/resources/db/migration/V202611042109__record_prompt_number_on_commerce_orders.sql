-- The number the M-Pesa prompt went to, so an order whose seat hold expires unpaid can be told by SMS.
ALTER TABLE commerce_order ADD COLUMN IF NOT EXISTS prompt_msisdn VARCHAR(16);

COMMENT ON COLUMN commerce_order.prompt_msisdn IS 'Normalised 2547XXXXXXXX the latest STK prompt was sent to; never shown unmasked';
