package apps.sarafrika.elimika.commerce.purchase.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CommercePurchaseItem> items = new ArrayList<>();
}
