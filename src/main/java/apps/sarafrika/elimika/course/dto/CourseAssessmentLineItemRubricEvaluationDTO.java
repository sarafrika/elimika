package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "CourseAssessmentLineItemRubricEvaluation", description = "Stored rubric evaluation for a learner against a gradebook line item")
public record CourseAssessmentLineItemRubricEvaluationDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty("line_item_uuid")
        UUID lineItemUuid,

        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @JsonProperty(value = "rubric_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID rubricUuid,

        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        CourseAssessmentLineItemRubricEvaluationStatus status,

        @JsonProperty(value = "attendance_status", access = JsonProperty.Access.READ_ONLY)
        CourseAttendanceStatus attendanceStatus,

        @JsonProperty(value = "score", access = JsonProperty.Access.READ_ONLY)
        BigDecimal score,

        @JsonProperty(value = "max_score", access = JsonProperty.Access.READ_ONLY)
        BigDecimal maxScore,

        @JsonProperty(value = "percentage", access = JsonProperty.Access.READ_ONLY)
        BigDecimal percentage,

        @JsonProperty("comments")
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        String comments,

        @JsonProperty("graded_at")
        LocalDateTime gradedAt,

        @JsonProperty("graded_by_uuid")
        UUID gradedByUuid,

        @JsonProperty("criteria_selections")
        @Valid
        List<CourseAssessmentLineItemRubricEvaluationRowDTO> criteriaSelections
) {

    @JsonProperty(value = "evaluation_display", access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "Completed: 8 / 10 (80%)")
    public String getEvaluationDisplay() {
        if (status == null) {
            return "No rubric evaluation";
        }
        if (status != CourseAssessmentLineItemRubricEvaluationStatus.COMPLETED || score == null || maxScore == null || percentage == null) {
            return "Pending rubric evaluation";
        }
        return "Completed: " + score.stripTrailingZeros().toPlainString()
                + " / " + maxScore.stripTrailingZeros().toPlainString()
                + " (" + percentage.stripTrailingZeros().toPlainString() + "%)";
    }
}
