package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * An instructor's manual grade for a single short-answer/essay quiz response.
 */
@Schema(
        name = "QuizManualGradeRequest",
        description = "Instructor grade for a short-answer or essay quiz response"
)
public record QuizManualGradeRequest(

        @Schema(
                description = "**[REQUIRED]** Points to award; clamped to the question's maximum.",
                example = "7.50",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Points are required")
        @DecimalMin(value = "0.0", message = "Points must be non-negative")
        @JsonProperty("points")
        BigDecimal points,

        @Schema(
                description = "**[OPTIONAL]** Whether the response is considered correct.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("is_correct")
        Boolean isCorrect,

        @Schema(
                description = "**[OPTIONAL]** Feedback for the student on this response.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("feedback")
        String feedback
) {
}
