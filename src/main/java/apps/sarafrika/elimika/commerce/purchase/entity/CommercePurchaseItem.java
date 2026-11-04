package apps.sarafrika.elimika.commerce.purchase.entity;

import apps.sarafrika.elimika.commerce.purchase.converter.PurchaseScopeConverter;
import apps.sarafrika.elimika.commerce.purchase.converter.SaleDeliveryKindConverter;
import apps.sarafrika.elimika.commerce.purchase.converter.SaleFeeModeConverter;
import apps.sarafrika.elimika.commerce.purchase.converter.SaleLegStatusConverter;
import apps.sarafrika.elimika.commerce.purchase.converter.SalePartyTypeConverter;
import apps.sarafrika.elimika.shared.spi.revenue.SaleDeliveryKind;
import apps.sarafrika.elimika.shared.spi.revenue.SaleFeeMode;
import apps.sarafrika.elimika.shared.spi.revenue.SaleLegStatus;
import apps.sarafrika.elimika.shared.spi.revenue.SalePartyType;
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
import java.time.OffsetDateTime;
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

    @Column(name = "creator_credited_amount")
    private BigDecimal creatorCreditedAmount;

    @Column(name = "creator_percentage")
    private BigDecimal creatorPercentage;

    @Convert(converter = SalePartyTypeConverter.class)
    @Column(name = "creator_owner_type")
    private SalePartyType creatorOwnerType;

    @Column(name = "creator_owner_uuid")
    private UUID creatorOwnerUuid;

    @Convert(converter = SaleLegStatusConverter.class)
    @Column(name = "creator_status")
    private SaleLegStatus creatorStatus;

    @Column(name = "creator_gross")
    private BigDecimal creatorGross;

    @Column(name = "creator_fee")
    private BigDecimal creatorFee;

    @Convert(converter = SaleDeliveryKindConverter.class)
    @Column(name = "delivery_kind")
    private SaleDeliveryKind deliveryKind;

    @Convert(converter = SalePartyTypeConverter.class)
    @Column(name = "delivery_owner_type")
    private SalePartyType deliveryOwnerType;

    @Column(name = "delivery_owner_uuid")
    private UUID deliveryOwnerUuid;

    @Convert(converter = SaleLegStatusConverter.class)
    @Column(name = "delivery_status")
    private SaleLegStatus deliveryStatus;

    @Column(name = "delivery_gross")
    private BigDecimal deliveryGross;

    @Column(name = "delivery_fee")
    private BigDecimal deliveryFee;

    @Column(name = "delivery_net")
    private BigDecimal deliveryNet;

    @Column(name = "fee_rule_uuid")
    private UUID feeRuleUuid;

    @Column(name = "fee_rule_key")
    private String feeRuleKey;

    @Convert(converter = SaleFeeModeConverter.class)
    @Column(name = "fee_mode")
    private SaleFeeMode feeMode;

    @Column(name = "fee_rate")
    private BigDecimal feeRate;

    @Column(name = "fee_rate_currency")
    private String feeRateCurrency;

    @Column(name = "fee_waived")
    private Boolean feeWaived;

    @Column(name = "fee_discount_percentage")
    private BigDecimal feeDiscountPercentage;

    @Column(name = "fee_discount_amount")
    private BigDecimal feeDiscountAmount;

    @Column(name = "fee_evaluated_at")
    private OffsetDateTime feeEvaluatedAt;

    /** When the line's split was settled into wallets (UTC). */
    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Column(name = "split_reconciles")
    private Boolean splitReconciles;

    @Column(name = "split_backfilled")
    private boolean splitBackfilled;

    @Column(name = "metadata_json")
    private String metadataJson;
}
