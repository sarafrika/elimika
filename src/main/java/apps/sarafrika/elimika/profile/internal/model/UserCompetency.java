package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_competencies")
@Getter
@Setter
@NoArgsConstructor
public class UserCompetency extends UserOwnedEntity implements VerifiableItem {

    @Column(name = "competency")
    @Filterable
    private String competency;

    @Column(name = "framework")
    @Filterable
    private String framework;

    @Column(name = "level")
    @Filterable
    private Integer level;

    @Column(name = "evidence")
    private String evidence;

    @Column(name = "verification_status")
    @Filterable
    private WalletVerificationStatus verificationStatus = WalletVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;
}
