package apps.sarafrika.elimika.commerce.internal.entity;

import apps.sarafrika.elimika.commerce.internal.converter.FulfillmentStatusConverter;
import apps.sarafrika.elimika.commerce.internal.converter.OrderStatusConverter;
import apps.sarafrika.elimika.commerce.internal.converter.PaymentStatusConverter;
import apps.sarafrika.elimika.commerce.internal.enums.FulfillmentStatus;
import apps.sarafrika.elimika.commerce.internal.enums.OrderStatus;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "commerce_order")
public class CommerceOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private CommerceCart cart;

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "customer_email")
    private String customerEmail;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "status")
    private OrderStatus status = OrderStatus.PENDING;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.AWAITING_PAYMENT;

    @Convert(converter = FulfillmentStatusConverter.class)
    @Column(name = "fulfillment_status")
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.NOT_FULFILLED;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "subtotal_amount")
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount")
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private String metadataJson;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<CommerceOrderItem> items;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<CommercePayment> payments;
}
