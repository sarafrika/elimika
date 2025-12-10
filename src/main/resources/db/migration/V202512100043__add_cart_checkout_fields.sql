-- Adds cart and order columns to store customer and payment context outside metadata.

ALTER TABLE commerce_cart
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS shipping_address_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS billing_address_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_provider_id VARCHAR(64);

ALTER TABLE commerce_order
    ADD COLUMN IF NOT EXISTS customer_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS shipping_address_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS billing_address_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_provider_id VARCHAR(64);
