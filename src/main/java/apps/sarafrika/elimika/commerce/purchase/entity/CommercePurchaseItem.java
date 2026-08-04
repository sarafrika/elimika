package apps.sarafrika.elimika.commerce.purchase.entity;

import apps.sarafrika.elimika.commerce.purchase.converter.PurchaseScopeConverter;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import java.math.BigDecimal;
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

    @Column(name = "line_item_id")
    private String lineItemId;

    @Column(name = "variant_id")
    private String variantId;

    @Column(name = "title")
    private String title;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Convert(converter = PurchaseScopeConverter.class)
    @Column(name = "scope")
    private PurchaseScope scope;

    /**
     * The order's platform fee apportioned to this line, taken off the top before the revenue share
     * is applied. Null on lines whose money was never settled - historical rows and uncaptured
     * orders - which is why the three settlement amounts are nullable and never backfilled.
     */
    @Column(name = "platform_fee_amount")
    private BigDecimal platformFeeAmount;

    /** What was credited to an earner's wallet for this line. Zero when nobody was credited. */
    @Column(name = "credited_amount")
    private BigDecimal creditedAmount;

    /** Collected, not taken as platform fee, and credited to no earner. */
    @Column(name = "retained_amount")
    private BigDecimal retainedAmount;

    @Column(name = "metadata_json")
    private String metadataJson;
}
