package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.coursecreator.util.enums.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_creator_skills")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorSkill extends BaseEntity {

    @Column(name = "course_creator_uuid")
    @Filterable
    private UUID courseCreatorUuid;

    @Column(name = "skill_name")
    @Filterable
    private String skillName;

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
    private LocalDate lastAssessedOn;

    @Column(name = "verification_status")
    @Filterable
    private WalletVerificationStatus verificationStatus = WalletVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;
}
