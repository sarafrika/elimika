package apps.sarafrika.elimika.commerce.purchase.entity;

import apps.sarafrika.elimika.commerce.purchase.converter.PurchaseScopeConverter;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commerce_purchase_item")
public class CommercePurchaseItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private CommercePurchase purchase;

    @Column(name = "medusa_line_item_id")
    private String medusaLineItemId;

    @Column(name = "variant_id")
    private String variantId;

    @Column(name = "title")
    private String title;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Convert(converter = PurchaseScopeConverter.class)
    @Column(name = "scope")
    private PurchaseScope scope;

    @Column(name = "metadata_json")
    private String metadataJson;
}
