package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.tenancy.util.enums.ConsentSource;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_organisation_domain_mapping")
@Getter @Setter @ToString
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public class UserOrganisationDomainMapping extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "domain_uuid")
    private UUID domainUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_uuid", referencedColumnName = "uuid",
            insertable = false, updatable = false)
    private UserDomain domain;

    @Column(name = "branch_uuid")
    private UUID branchUuid;

    @Column(name = "active")
    private boolean active;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "deleted")
    private boolean deleted;

    // ================================
    // CONSENT
    // ================================
    // An affiliation must be traceable to a consenting act. Rows created before consent
    // capture carry ConsentSource.LEGACY and a null timestamp rather than implying a
    // consent that was never given.

    @Column(name = "consent_granted_at")
    private LocalDateTime consentGrantedAt;

    @Column(name = "consent_source")
    private ConsentSource consentSource;

    /** Who performed the consenting act - the user themselves, or a guardian for a minor. */
    @Column(name = "consent_granted_by_user_uuid")
    private UUID consentGrantedByUserUuid;

    @Column(name = "invitation_uuid")
    private UUID invitationUuid;
}