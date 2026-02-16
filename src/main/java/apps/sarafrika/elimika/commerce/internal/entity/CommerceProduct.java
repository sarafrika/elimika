package apps.sarafrika.elimika.commerce.internal.entity;

import apps.sarafrika.elimika.commerce.internal.converter.ProductStatusConverter;
import apps.sarafrika.elimika.commerce.internal.enums.ProductStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commerce_product")
public class CommerceProduct extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "currency_code")
    private String currencyCode;

    @Convert(converter = ProductStatusConverter.class)
    @Column(name = "status")
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "active")
    private boolean active = true;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<CommerceProductVariant> variants;
}
