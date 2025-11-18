package apps.sarafrika.elimika.commerce.internal.entity;

import apps.sarafrika.elimika.commerce.internal.converter.VariantStatusConverter;
import apps.sarafrika.elimika.commerce.internal.enums.VariantStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "commerce_product_variant")
public class CommerceProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private CommerceProduct product;

    @Column(name = "code")
    private String code;

    @Column(name = "title")
    private String title;

    @Column(name = "unit_amount")
    private BigDecimal unitAmount = BigDecimal.ZERO;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "inventory_quantity")
    private Integer inventoryQuantity = 0;

    @Convert(converter = VariantStatusConverter.class)
    @Column(name = "status")
    private VariantStatus status = VariantStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private String metadataJson;
}
