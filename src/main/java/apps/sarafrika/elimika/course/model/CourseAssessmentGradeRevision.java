package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.CourseAssessmentLineItemTypeConverter;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One grade given to a learner on a line item. Never updated: a regrade, a new quiz attempt or
 * an attendance sync adds the next revision, so every attempt stays visible.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_assessment_grade_revisions")
public class CourseAssessmentGradeRevision extends BaseEntity {

    /** A rubric pick as it stood when the grade was given. */
    public record Criterion(UUID criteriaUuid, String criteriaName, UUID scoringLevelUuid,
                            String scoringLevelName, BigDecimal points, String comments) {
    }

    @Column(name = "line_item_uuid")
    private UUID lineItemUuid;

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "revision_number")
    private Integer revisionNumber;

    @Column(name = "item_type")
    @Convert(converter = CourseAssessmentLineItemTypeConverter.class)
    private CourseAssessmentLineItemType itemType;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "comments")
    private String comments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria")
    private List<Criterion> criteria;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by_uuid")
    private UUID gradedByUuid;
}
