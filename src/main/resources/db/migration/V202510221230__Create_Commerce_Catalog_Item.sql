CREATE TABLE IF NOT EXISTS commerce_catalog_item
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid           UUID,
    class_definition_uuid UUID,
    medusa_product_id     VARCHAR(64) NOT NULL,
    medusa_variant_id     VARCHAR(64) NOT NULL,
    currency_code         VARCHAR(16),
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    created_date          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50) NOT NULL,
    updated_date          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    CONSTRAINT chk_course_or_class CHECK (
        (course_uuid IS NOT NULL) OR (class_definition_uuid IS NOT NULL)
    ),
    CONSTRAINT uq_catalog_course UNIQUE (course_uuid),
    CONSTRAINT uq_catalog_class UNIQUE (class_definition_uuid),
    CONSTRAINT uq_catalog_variant UNIQUE (medusa_variant_id),
    CONSTRAINT fk_catalog_course FOREIGN KEY (course_uuid) REFERENCES courses (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_catalog_class FOREIGN KEY (class_definition_uuid) REFERENCES class_definitions (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_catalog_course_uuid ON commerce_catalog_item (course_uuid);
CREATE INDEX idx_catalog_class_uuid ON commerce_catalog_item (class_definition_uuid);
CREATE INDEX idx_catalog_active ON commerce_catalog_item (active);
