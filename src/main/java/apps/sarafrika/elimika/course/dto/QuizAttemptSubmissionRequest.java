package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Payload for submitting a quiz attempt for grading. Any answers included here are saved
 * before grading, so a client may submit all answers in a single call or rely on answers
 * saved earlier via the responses endpoint.
 */
@Schema(
        name = "QuizAttemptSubmissionRequest",
        description = "Final answers and timing for a quiz attempt being submitted for grading"
)
public record QuizAttemptSubmissionRequest(

        @Schema(
                description = "**[OPTIONAL]** Final answers to save before grading. May be omitted if answers were already saved.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Valid
        @JsonProperty("responses")
        List<QuizResponseSubmissionDTO> responses,

        @Schema(
                description = "**[OPTIONAL]** Time the student spent on the attempt, in minutes. Computed from timestamps when omitted.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("time_taken_minutes")
        Integer timeTakenMinutes
) {
}
