package apps.sarafrika.elimika.commerce.internal.entity;

import apps.sarafrika.elimika.commerce.internal.converter.PaymentStatusConverter;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commerce_payment")
public class CommercePayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CommerceOrder order;

    @Column(name = "provider")
    private String provider;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "status")
    private PaymentStatus status = PaymentStatus.AWAITING_PAYMENT;

    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private String metadataJson;
}
