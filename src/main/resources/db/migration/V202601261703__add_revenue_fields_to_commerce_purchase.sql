ALTER TABLE commerce_purchase
    ADD COLUMN order_currency_code VARCHAR(16),
    ADD COLUMN order_subtotal_amount NUMERIC(19, 4),
    ADD COLUMN order_total_amount NUMERIC(19, 4);

ALTER TABLE commerce_purchase_item
    ADD COLUMN unit_price NUMERIC(19, 4),
    ADD COLUMN subtotal NUMERIC(19, 4),
    ADD COLUMN total NUMERIC(19, 4);
