package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CourseAssessmentLineItemScore",
        description = "Learner score stored against a gradebook line item",
        example = """
        {
            "uuid": "sc1o2r3e-4i5t-6e7m-8a9b-abcdefghijkl",
            "line_item_uuid": "li1a2n3e-4i5t-6e7m-8a9b-abcdefghijkl",
            "enrollment_uuid": "e1n2r3o4-5l6l-7m8e-9n10-abcdefghijkl",
            "score": 18.00,
            "max_score": 20.00,
            "percentage": 90.00,
            "comments": "Strong quiz performance"
        }
        """
)
public record CourseAssessmentLineItemScoreDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        UUID uuid,

        @JsonProperty("line_item_uuid")
        UUID lineItemUuid,

        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @JsonProperty("score")
        @DecimalMin(value = "0.0", message = "Score must be non-negative")
        BigDecimal score,

        @JsonProperty("max_score")
        @DecimalMin(value = "0.01", message = "Maximum score must be positive when provided")
        BigDecimal maxScore,

        @JsonProperty("percentage")
        @DecimalMin(value = "0.0", message = "Percentage must be non-negative")
        @DecimalMax(value = "100.0", message = "Percentage must not exceed 100")
        BigDecimal percentage,

        @JsonProperty("comments")
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        String comments,

        @JsonProperty("graded_at")
        LocalDateTime gradedAt,

        @JsonProperty("graded_by_uuid")
        UUID gradedByUuid,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

    @JsonProperty(value = "grade_display", access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "18 / 20 (90%)")
    public String getGradeDisplay() {
        if (score == null || maxScore == null || percentage == null) {
            return "Ungraded";
        }

        return score.stripTrailingZeros().toPlainString() + " / "
                + maxScore.stripTrailingZeros().toPlainString() + " ("
                + percentage.stripTrailingZeros().toPlainString() + "%)";
    }
}
