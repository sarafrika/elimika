-- Names the buyer on carts and orders that were written before the buyer was recorded.
--
-- Access to a cart or an order is now decided by the user_uuid on the row: its buyer sees it and
-- nobody else does. Carts only began recording the buyer at creation in 6c139d4e and no backfill
-- followed, so every earlier row is unattributed — and an unattributed row is either invisible to
-- the person who actually paid for it, or, if it were handed to whoever presents its id, a free
-- gift to anyone who came by that id. Attributing the rows removes both.
--
-- Every purchase is made by a signed-in learner, so the receipt email on these rows is an account
-- email; users.email is unique, so each match names exactly one person. What has no matching
-- account cannot be attributed by any evidence the platform holds and is deliberately left NULL:
-- such a row stays visible only to an administrator.

UPDATE commerce_cart c
SET user_uuid = u.uuid
FROM users u
WHERE c.user_uuid IS NULL
  AND c.customer_email IS NOT NULL
  AND lower(c.customer_email) = lower(u.email);

-- An order is placed from a cart and inherits its buyer, so a cart attributed above (or one that
-- always carried its buyer) settles the order hanging off it.
UPDATE commerce_order o
SET user_uuid = c.user_uuid
FROM commerce_cart c
WHERE o.user_uuid IS NULL
  AND o.cart_id = c.id
  AND c.user_uuid IS NOT NULL;

-- Orders whose cart was since deleted (fk_order_cart is ON DELETE SET NULL) still carry the
-- receipt email that was required to complete the checkout.
UPDATE commerce_order o
SET user_uuid = u.uuid
FROM users u
WHERE o.user_uuid IS NULL
  AND lower(o.customer_email) = lower(u.email);
