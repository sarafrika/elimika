package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.utils.Filterable;
import apps.sarafrika.elimika.shared.utils.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_skills")
@Getter
@Setter
@NoArgsConstructor
public class UserSkill extends UserOwnedEntity implements VerifiableItem {

    @Column(name = "skill_name")
    @Filterable
    private String skillName;

    /** The taxonomy skill the name resolves to; null while the name is free text only. */
    @Column(name = "skill_uuid")
    @Filterable
    private UUID skillUuid;

    @Column(name = "proficiency_level")
    @Convert(converter = ProficiencyLevelConverter.class)
    @Filterable
    private ProficiencyLevel proficiencyLevel;

    @Column(name = "evidence")
    private String evidence;

    @Column(name = "last_assessed_on")
    @Filterable
    private LocalDate lastAssessedOn;

    @Column(name = "verification_status")
    @Filterable
    private WalletVerificationStatus verificationStatus = WalletVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;
}
