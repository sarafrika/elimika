package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** One piece of skill evidence from a graded line item; never edited, only superseded by a regrade. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "learner_skill_evidence")
public class LearnerSkillEvidence extends BaseEntity {

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "skill_uuid")
    private UUID skillUuid;

    @Column(name = "course_enrollment_uuid")
    private UUID courseEnrollmentUuid;

    @Column(name = "course_assessment_uuid")
    private UUID courseAssessmentUuid;

    @Column(name = "line_item_uuid")
    private UUID lineItemUuid;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "level_name")
    private String levelName;

    @Column(name = "counts_as_achieved")
    private Boolean countsAsAchieved;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by_uuid")
    private UUID gradedByUuid;

    @Column(name = "supersedes_uuid")
    private UUID supersedesUuid;

    @Column(name = "superseded")
    private Boolean superseded = Boolean.FALSE;
}
