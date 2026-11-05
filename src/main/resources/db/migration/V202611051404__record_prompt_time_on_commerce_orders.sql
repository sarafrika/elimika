-- When the latest M-Pesa prompt for an order went out. The seat hold and the give-up window run
-- from here, so a resent prompt gets its full time instead of whatever was left since checkout.
ALTER TABLE commerce_order ADD COLUMN prompt_sent_at TIMESTAMPTZ;
UPDATE commerce_order SET prompt_sent_at = placed_at WHERE checkout_request_id IS NOT NULL AND prompt_sent_at IS NULL;
