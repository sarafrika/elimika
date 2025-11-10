package apps.sarafrika.elimika.commerce.purchase.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commerce_purchase")
public class CommercePurchase extends BaseEntity {

    @Column(name = "medusa_order_id")
    private String medusaOrderId;

    @Column(name = "medusa_display_id")
    private String medusaDisplayId;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "user_uuid")
    private java.util.UUID userUuid;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "medusa_created_at")
    private OffsetDateTime medusaCreatedAt;

    @Column(name = "platform_fee_amount")
    private BigDecimal platformFeeAmount;

    @Column(name = "platform_fee_currency")
    private String platformFeeCurrency;

    @Column(name = "platform_fee_rule_uuid")
    private UUID platformFeeRuleUuid;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CommercePurchaseItem> items = new ArrayList<>();
}
