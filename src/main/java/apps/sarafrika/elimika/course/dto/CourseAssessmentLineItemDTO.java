package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CourseAssessmentLineItem",
        description = "Gradebook line item nested under a weighted course assessment component",
        example = """
        {
            "uuid": "li1a2n3e-4i5t-6e7m-8a9b-abcdefghijkl",
            "course_assessment_uuid": "c1a2s3s4-5e6s-7s8m-9e10-abcdefghijkl",
            "title": "Quiz 1",
            "description": "Foundational knowledge check",
            "item_type": "discussion",
            "quiz_uuid": "q1u2i3z4-5u6u-7i8d-9q10-abcdefghijkl",
            "max_score": 20.00,
            "weight_percentage": 25.00,
            "display_order": 1,
            "active": true,
            "due_at": "2024-04-20T09:00:00"
        }
        """
)
public record CourseAssessmentLineItemDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        UUID uuid,

        @JsonProperty("course_assessment_uuid")
        UUID courseAssessmentUuid,

        @JsonProperty("title")
        @NotBlank(message = "Line item title is required")
        @Size(max = 255, message = "Line item title must not exceed 255 characters")
        String title,

        @JsonProperty("description")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @JsonProperty("item_type")
        @NotNull(message = "Line item type is required")
        CourseAssessmentLineItemType itemType,

        @JsonProperty("assignment_uuid")
        UUID assignmentUuid,

        @JsonProperty("quiz_uuid")
        UUID quizUuid,

        @JsonProperty("rubric_uuid")
        UUID rubricUuid,

        @JsonProperty("max_score")
        @DecimalMin(value = "0.01", message = "Maximum score must be positive when provided")
        BigDecimal maxScore,

        @JsonProperty("weight_percentage")
        @DecimalMin(value = "0.01", message = "Line item weight must be positive when provided")
        @DecimalMax(value = "100.00", message = "Line item weight must not exceed 100")
        BigDecimal weightPercentage,

        @JsonProperty("display_order")
        Integer displayOrder,

        @JsonProperty("active")
        Boolean active,

        @JsonProperty("due_at")
        LocalDateTime dueAt,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

    @JsonProperty(value = "item_type_display", access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "Quiz line item")
    public String getItemTypeDisplay() {
        if (itemType == null) {
            return "Unspecified line item";
        }

        return switch (itemType) {
            case ASSIGNMENT -> "Assignment line item";
            case QUIZ -> "Quiz line item";
            case ATTENDANCE -> "Attendance line item";
            case PROJECT -> "Project line item";
            case DISCUSSION -> "Discussion line item";
            case EXAM -> "Exam line item";
            case PRACTICAL -> "Practical line item";
            case PERFORMANCE -> "Performance line item";
            case PARTICIPATION -> "Participation line item";
            case MANUAL -> "Manual line item";
        };
    }
}
