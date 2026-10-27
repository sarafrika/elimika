package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** One graded source of evidence for a programme skill: a rubric criterion, or the whole score when the criterion is null. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "program_skill_mappings")
public class ProgramSkillMapping extends BaseEntity {

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "skill_uuid")
    private UUID skillUuid;

    @Column(name = "course_assessment_uuid")
    private UUID courseAssessmentUuid;

    @Column(name = "rubric_criteria_uuid")
    private UUID rubricCriteriaUuid;

    @Column(name = "share_percentage")
    private BigDecimal sharePercentage;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
