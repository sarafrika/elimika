package apps.sarafrika.elimika.commerce.catalog.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commerce_catalog_item")
public class CommerceCatalogItem extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "medusa_product_id")
    private String medusaProductId;

    @Column(name = "medusa_variant_id")
    private String medusaVariantId;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "active")
    private boolean active = true;
}
