package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.CourseAssessmentLineItemRubricEvaluationStatusConverter;
import apps.sarafrika.elimika.course.util.converter.CourseAttendanceStatusConverter;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;
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
@Table(name = "course_assessment_line_item_rubric_evaluations")
public class CourseAssessmentLineItemRubricEvaluation extends BaseEntity {

    @Column(name = "line_item_uuid")
    private UUID lineItemUuid;

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "status")
    @Convert(converter = CourseAssessmentLineItemRubricEvaluationStatusConverter.class)
    private CourseAssessmentLineItemRubricEvaluationStatus status;

    @Column(name = "attendance_status")
    @Convert(converter = CourseAttendanceStatusConverter.class)
    private CourseAttendanceStatus attendanceStatus;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "comments")
    private String comments;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by_uuid")
    private UUID gradedByUuid;
}
