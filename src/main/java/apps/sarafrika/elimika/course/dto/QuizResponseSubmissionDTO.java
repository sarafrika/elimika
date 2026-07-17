package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A single answer a student provides for one quiz question while taking a quiz.
 * <p>
 * For multiple-choice / true-false questions supply {@code selected_option_uuid};
 * for short-answer / essay questions supply {@code text_response}. Scoring and
 * correctness are computed server-side at submission time and are never accepted here.
 */
@Schema(
        name = "QuizResponseSubmission",
        description = "A student's answer to a single quiz question"
)
public record QuizResponseSubmissionDTO(

        @Schema(
                description = "**[REQUIRED]** UUID of the question being answered.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Question UUID is required")
        @JsonProperty("question_uuid")
        UUID questionUuid,

        @Schema(
                description = "**[OPTIONAL]** Selected option UUID for multiple-choice or true/false questions.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("selected_option_uuid")
        UUID selectedOptionUuid,

        @Schema(
                description = "**[OPTIONAL]** Free-text answer for short-answer or essay questions.",
                nullable = true,
                maxLength = 5000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 5000, message = "Text response must not exceed 5000 characters")
        @JsonProperty("text_response")
        String textResponse
) {
}
