package apps.sarafrika.elimika.shared.model;

import apps.sarafrika.elimika.shared.internal.DatabaseAuditListener;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, DatabaseAuditListener.class})
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @UuidGenerator
    @Column(name = "uuid")
    @Filterable
    private UUID uuid;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_date")
    @Filterable
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false, name = "updated_date")
    @Filterable
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(nullable = false, updatable = false, name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(insertable = false, name = "updated_by")
    private String lastModifiedBy;
}
