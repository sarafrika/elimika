package apps.sarafrika.elimika.tenancy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_domain_mapping")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDomainMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "domain_uuid")
    private UUID userDomainUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_uuid", referencedColumnName = "uuid",
            insertable = false, updatable = false)
    private UserDomain userDomain;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}