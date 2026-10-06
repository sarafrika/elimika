package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "CourseGradeBook",
        description = "Enrollment-gradebook view showing weighted components, line items, and aggregate grade"
)
public record CourseGradeBookDTO(

        @JsonProperty("course_uuid")
        UUID courseUuid,

        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @JsonProperty("final_grade")
        BigDecimal finalGrade,

        @Schema(description = "Final grade needed to pass; null means completing every required item passes")
        @JsonProperty("pass_mark")
        BigDecimal passMark,

        @Schema(description = "IN_PROGRESS until every required item is graded, then PASSED or FAILED")
        @JsonProperty("result_status")
        CourseResultStatus resultStatus,

        @JsonProperty("graded_weight_percentage")
        BigDecimal gradedWeightPercentage,

        @JsonProperty("configured_weight_percentage")
        BigDecimal configuredWeightPercentage,

        @JsonProperty("components")
        List<ComponentDTO> components
) {

    public record ComponentDTO(
            @JsonProperty("assessment") CourseAssessmentDTO assessment,
            @JsonProperty("aggregate_score") CourseAssessmentScoreDTO aggregateScore,
            @JsonProperty("configured_line_item_weight_percentage") BigDecimal configuredLineItemWeightPercentage,
            @JsonProperty("line_items") List<LineItemEntryDTO> lineItems
    ) {
    }

    public record LineItemEntryDTO(
            @JsonProperty("line_item") CourseAssessmentLineItemDTO lineItem,
            @JsonProperty("score") CourseAssessmentLineItemScoreDTO score
    ) {
    }
}
