package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_assessment_line_item_rubric_evaluation_rows")
public class CourseAssessmentLineItemRubricEvaluationRow extends BaseEntity {

    @Column(name = "evaluation_uuid")
    private UUID evaluationUuid;

    @Column(name = "criteria_uuid")
    private UUID criteriaUuid;

    @Column(name = "scoring_level_uuid")
    private UUID scoringLevelUuid;

    @Column(name = "points")
    private BigDecimal points;

    @Column(name = "comments")
    private String comments;
}
