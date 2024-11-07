package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoursePricing extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal basePrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date discountStart;

    @Temporal(TemporalType.TIMESTAMP)
    private Date discountEnd;

    @Column(length = 50)
    private String discountCode;

    @Column(insertable = false, updatable = false)
    private BigDecimal finalPrice;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    private boolean free;

    @Column(nullable = false)
    private Long courseId;
}
