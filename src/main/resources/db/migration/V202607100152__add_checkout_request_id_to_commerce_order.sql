ALTER TABLE commerce_order
    ADD COLUMN checkout_request_id VARCHAR(255);

COMMENT ON COLUMN commerce_order.checkout_request_id IS 'M-Pesa STK Push checkout request id used to poll payment status against the mpesa-service gateway';
