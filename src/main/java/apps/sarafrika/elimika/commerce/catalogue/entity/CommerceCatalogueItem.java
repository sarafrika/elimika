package apps.sarafrika.elimika.commerce.catalogue.entity;

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
@Table(name = "commerce_catalogue_item")
public class CommerceCatalogueItem extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "variant_code")
    private String variantCode;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "publicly_visible")
    private boolean publiclyVisible = true;
}
