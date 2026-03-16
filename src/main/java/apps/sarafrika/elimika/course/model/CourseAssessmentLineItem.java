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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_assessment_line_items")
public class CourseAssessmentLineItem extends BaseEntity {

    @Column(name = "course_assessment_uuid")
    private UUID courseAssessmentUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "item_type")
    @Convert(converter = CourseAssessmentLineItemTypeConverter.class)
    private CourseAssessmentLineItemType itemType;

    @Column(name = "assignment_uuid")
    private UUID assignmentUuid;

    @Column(name = "quiz_uuid")
    private UUID quizUuid;

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "weight_percentage")
    private BigDecimal weightPercentage;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "due_at")
    private LocalDateTime dueAt;
}
