package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.search.SearchIndexingEntityListener;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A standalone (organisation-independent) domain held by a user. Not a {@code BaseEntity}, so it
 * registers the search listener itself: the {@code people} index projects these domains.
 */
@Entity
@EntityListeners(SearchIndexingEntityListener.class)
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

    /** Fails closed: flows that grant access outright set APPROVED themselves. */
    @Column(name = "status")
    private DomainApprovalStatus status = DomainApprovalStatus.PENDING;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "review_reason")
    private String reviewReason;

    /** When the user submitted this domain's onboarding (finished it, for domains without review). */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static UserDomainMapping of(UUID userUuid, UUID domainUuid, DomainApprovalStatus status) {
        UserDomainMapping mapping = new UserDomainMapping();
        mapping.setUserUuid(userUuid);
        mapping.setUserDomainUuid(domainUuid);
        mapping.setStatus(status);
        return mapping;
    }

    public boolean isApproved() {
        return status == DomainApprovalStatus.APPROVED;
    }
}
