package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** The course creator's notes when approving or rejecting a proposed rate update. */
@Schema(
        name = "TrainingRateUpdateDecisionRequest",
        description = "Payload for approving or rejecting a proposed training rate update",
        example = """
        {
          "review_notes": "Approved from next month's classes."
        }
        """
)
public record TrainingRateUpdateDecisionRequest(

        @Schema(description = "Optional notes shown to the applicant.", maxLength = 2000, nullable = true)
        @JsonProperty("review_notes")
        @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
        String reviewNotes
) {
}
