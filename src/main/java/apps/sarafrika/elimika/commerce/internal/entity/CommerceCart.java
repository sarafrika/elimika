package apps.sarafrika.elimika.commerce.internal.entity;

import apps.sarafrika.elimika.commerce.internal.converter.CartStatusConverter;
import apps.sarafrika.elimika.commerce.internal.enums.CartStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commerce_cart")
public class CommerceCart extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Convert(converter = CartStatusConverter.class)
    @Column(name = "status")
    private CartStatus status = CartStatus.OPEN;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "shipping_address_id")
    private String shippingAddressId;

    @Column(name = "billing_address_id")
    private String billingAddressId;

    @Column(name = "payment_provider_id")
    private String paymentProviderId;

    @Column(name = "subtotal_amount")
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount")
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private String metadataJson;

    @OneToMany(mappedBy = "cart", fetch = FetchType.LAZY)
    private List<CommerceCartItem> items;
}
