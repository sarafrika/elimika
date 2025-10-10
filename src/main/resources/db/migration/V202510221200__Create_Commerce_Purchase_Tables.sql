-- Creates tables for recording commerce purchases and line items linked to Medusa orders.

CREATE TABLE IF NOT EXISTS commerce_purchase
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    medusa_order_id    VARCHAR(64) NOT NULL UNIQUE,
    medusa_display_id  VARCHAR(64),
    customer_email     VARCHAR(255),
    user_uuid          UUID,
    payment_status     VARCHAR(64),
    medusa_created_at  TIMESTAMPTZ,
    created_date       TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50) NOT NULL,
    updated_date       TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_commerce_purchase_user FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE SET NULL
);

CREATE INDEX idx_commerce_purchase_payment_status ON commerce_purchase (payment_status);
CREATE INDEX idx_commerce_purchase_user_uuid ON commerce_purchase (user_uuid);

CREATE TABLE IF NOT EXISTS commerce_purchase_item
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    purchase_id           BIGINT      NOT NULL,
    medusa_line_item_id   VARCHAR(64) NOT NULL,
    variant_id            VARCHAR(64),
    title                 TEXT,
    quantity              INTEGER     NOT NULL        DEFAULT 1,
    student_uuid          UUID,
    course_uuid           UUID,
    class_definition_uuid UUID,
    scope                 VARCHAR(16),
    metadata_json         TEXT,
    created_date          TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50) NOT NULL,
    updated_date          TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    CONSTRAINT fk_purchase_item_purchase FOREIGN KEY (purchase_id) REFERENCES commerce_purchase (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_item_student FOREIGN KEY (student_uuid) REFERENCES students (uuid) ON DELETE SET NULL,
    CONSTRAINT fk_purchase_item_course FOREIGN KEY (course_uuid) REFERENCES courses (uuid) ON DELETE SET NULL,
    CONSTRAINT fk_purchase_item_class FOREIGN KEY (class_definition_uuid) REFERENCES class_definitions (uuid) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uk_commerce_purchase_item_line_item ON commerce_purchase_item (medusa_line_item_id);
CREATE INDEX idx_commerce_purchase_item_student ON commerce_purchase_item (student_uuid);
CREATE INDEX idx_commerce_purchase_item_course ON commerce_purchase_item (course_uuid);
CREATE INDEX idx_commerce_purchase_item_class ON commerce_purchase_item (class_definition_uuid);
CREATE INDEX idx_commerce_purchase_item_scope ON commerce_purchase_item (scope);

