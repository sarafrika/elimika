package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.CredentialType;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_certifications")
@Getter
@Setter
@NoArgsConstructor
public class UserCertification extends UserOwnedEntity implements VerifiableItem {

    @Column(name = "certification_name")
    @Filterable
    private String certificationName;

    @Column(name = "issuing_organization")
    @Filterable
    private String issuingOrganization;

    @Column(name = "issued_date")
    @Filterable
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    @Filterable
    private LocalDate expiryDate;

    @Column(name = "credential_id")
    @Filterable
    private String credentialId;

    @Column(name = "credential_url")
    private String credentialUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "credential_type")
    @Filterable
    private CredentialType credentialType;

    @Column(name = "verification_status")
    @Filterable
    private WalletVerificationStatus verificationStatus = WalletVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;
}
