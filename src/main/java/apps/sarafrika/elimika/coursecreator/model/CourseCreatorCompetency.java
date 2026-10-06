package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.coursecreator.util.enums.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_creator_competencies")
@Getter
@Setter
@NoArgsConstructor
public class CourseCreatorCompetency extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "competency")
    private String competency;

    @Column(name = "framework")
    private String framework;

    @Column(name = "level")
    private Integer level;

    @Column(name = "evidence")
    private String evidence;

    @Column(name = "verification_status")
    private WalletVerificationStatus verificationStatus = WalletVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_notes")
    private String verificationNotes;
}
