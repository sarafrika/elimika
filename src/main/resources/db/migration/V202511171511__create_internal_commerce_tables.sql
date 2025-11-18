-- Introduces internal commerce tables to replace the Medusa-backed catalog, cart, and order flows.

CREATE TABLE IF NOT EXISTS commerce_product
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid           UUID,
    class_definition_uuid UUID,
    title                 TEXT         NOT NULL,
    description           TEXT,
    currency_code         VARCHAR(3)   NOT NULL,
    status                VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50)  NOT NULL,
    updated_date          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    CONSTRAINT chk_product_course_or_class CHECK (
        (course_uuid IS NOT NULL) OR (class_definition_uuid IS NOT NULL)
    ),
    CONSTRAINT uq_product_course UNIQUE (course_uuid),
    CONSTRAINT uq_product_class UNIQUE (class_definition_uuid),
    CONSTRAINT fk_product_course FOREIGN KEY (course_uuid) REFERENCES courses (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_product_class FOREIGN KEY (class_definition_uuid) REFERENCES class_definitions (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_product_status ON commerce_product (status);
CREATE INDEX idx_product_active ON commerce_product (active);

CREATE TABLE IF NOT EXISTS commerce_product_variant
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    product_id         BIGINT      NOT NULL,
    code               VARCHAR(64) NOT NULL,
    title              TEXT        NOT NULL,
    unit_amount        NUMERIC(18,4) NOT NULL,
    currency_code      VARCHAR(3)  NOT NULL,
    inventory_quantity INTEGER     NOT NULL DEFAULT 0,
    status             VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json      JSONB,
    created_date       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50) NOT NULL,
    updated_date       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES commerce_product (id) ON DELETE CASCADE,
    CONSTRAINT uq_variant_code UNIQUE (code),
    CONSTRAINT chk_variant_qty_non_negative CHECK (inventory_quantity >= 0),
    CONSTRAINT chk_variant_amount_non_negative CHECK (unit_amount >= 0)
);

CREATE INDEX idx_variant_product_id ON commerce_product_variant (product_id);
CREATE INDEX idx_variant_status ON commerce_product_variant (status);

CREATE TABLE IF NOT EXISTS commerce_cart
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_uuid        UUID,
    status           VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
    currency_code    VARCHAR(3)   NOT NULL,
    region_code      VARCHAR(32),
    subtotal_amount  NUMERIC(18,4) NOT NULL DEFAULT 0,
    tax_amount       NUMERIC(18,4) NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(18,4) NOT NULL DEFAULT 0,
    shipping_amount  NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_amount     NUMERIC(18,4) NOT NULL DEFAULT 0,
    expires_at       TIMESTAMPTZ,
    metadata_json    JSONB,
    created_date     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50)  NOT NULL,
    updated_date     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(50),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,
    CONSTRAINT chk_cart_amounts_non_negative CHECK (
        subtotal_amount >= 0
        AND tax_amount >= 0
        AND discount_amount >= 0
        AND shipping_amount >= 0
        AND total_amount >= 0
    )
);

CREATE INDEX idx_cart_user_uuid ON commerce_cart (user_uuid);
CREATE INDEX idx_cart_status ON commerce_cart (status);
CREATE INDEX idx_cart_expires_at ON commerce_cart (expires_at);

CREATE TABLE IF NOT EXISTS commerce_cart_item
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    cart_id            BIGINT       NOT NULL,
    product_variant_id BIGINT       NOT NULL,
    quantity           INTEGER      NOT NULL DEFAULT 1,
    unit_amount        NUMERIC(18,4) NOT NULL,
    subtotal_amount    NUMERIC(18,4) NOT NULL,
    total_amount       NUMERIC(18,4) NOT NULL,
    metadata_json      JSONB,
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50)  NOT NULL,
    updated_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES commerce_cart (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_variant FOREIGN KEY (product_variant_id) REFERENCES commerce_product_variant (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_cart_item_amounts CHECK (
        unit_amount >= 0
        AND subtotal_amount >= 0
        AND total_amount >= 0
    )
);

CREATE INDEX idx_cart_item_cart_id ON commerce_cart_item (cart_id);
CREATE INDEX idx_cart_item_variant_id ON commerce_cart_item (product_variant_id);

CREATE TABLE IF NOT EXISTS commerce_order
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    cart_id            BIGINT,
    user_uuid          UUID,
    customer_email     VARCHAR(255) NOT NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    payment_status     VARCHAR(32)  NOT NULL DEFAULT 'AWAITING_PAYMENT',
    fulfillment_status VARCHAR(32)  NOT NULL DEFAULT 'NOT_FULFILLED',
    currency_code      VARCHAR(3)   NOT NULL,
    subtotal_amount    NUMERIC(18,4) NOT NULL,
    tax_amount         NUMERIC(18,4) NOT NULL DEFAULT 0,
    shipping_amount    NUMERIC(18,4) NOT NULL DEFAULT 0,
    discount_amount    NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_amount       NUMERIC(18,4) NOT NULL,
    placed_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata_json      JSONB,
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50)  NOT NULL,
    updated_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_order_cart FOREIGN KEY (cart_id) REFERENCES commerce_cart (id) ON DELETE SET NULL,
    CONSTRAINT fk_order_user FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE SET NULL,
    CONSTRAINT chk_order_amounts CHECK (
        subtotal_amount >= 0
        AND tax_amount >= 0
        AND shipping_amount >= 0
        AND discount_amount >= 0
        AND total_amount >= 0
    )
);

CREATE INDEX idx_order_status ON commerce_order (status);
CREATE INDEX idx_order_payment_status ON commerce_order (payment_status);
CREATE INDEX idx_order_user_uuid ON commerce_order (user_uuid);
CREATE INDEX idx_order_customer_email ON commerce_order (customer_email);

CREATE TABLE IF NOT EXISTS commerce_order_item
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    order_id           BIGINT       NOT NULL,
    product_variant_id BIGINT       NOT NULL,
    quantity           INTEGER      NOT NULL DEFAULT 1,
    unit_amount        NUMERIC(18,4) NOT NULL,
    subtotal_amount    NUMERIC(18,4) NOT NULL,
    total_amount       NUMERIC(18,4) NOT NULL,
    title              TEXT,
    metadata_json      JSONB,
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50)  NOT NULL,
    updated_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES commerce_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_variant FOREIGN KEY (product_variant_id) REFERENCES commerce_product_variant (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_amounts CHECK (
        unit_amount >= 0
        AND subtotal_amount >= 0
        AND total_amount >= 0
    )
);

CREATE INDEX idx_order_item_order_id ON commerce_order_item (order_id);
CREATE INDEX idx_order_item_variant_id ON commerce_order_item (product_variant_id);

CREATE TABLE IF NOT EXISTS commerce_payment
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    order_id           BIGINT       NOT NULL,
    provider           VARCHAR(64)  NOT NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'AWAITING_CONFIRMATION',
    amount             NUMERIC(18,4) NOT NULL,
    currency_code      VARCHAR(3)   NOT NULL,
    external_reference VARCHAR(128),
    processed_at       TIMESTAMPTZ,
    metadata_json      JSONB,
    created_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50)  NOT NULL,
    updated_date       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES commerce_order (id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
);

CREATE INDEX idx_payment_order_id ON commerce_payment (order_id);
CREATE INDEX idx_payment_status ON commerce_payment (status);
CREATE INDEX idx_payment_provider ON commerce_payment (provider);
