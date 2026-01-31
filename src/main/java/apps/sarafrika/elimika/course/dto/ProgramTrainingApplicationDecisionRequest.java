package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request payload used by program creators when approving or rejecting applications.
 */
@Schema(
        name = "ProgramTrainingApplicationDecisionRequest",
        description = "Payload for approving or rejecting a training program application",
        example = """
        {
          "review_notes": "Approved for the 2025 Q1 intake."
        }
        """
)
public record ProgramTrainingApplicationDecisionRequest(

        @Schema(
                description = "Optional notes captured alongside the decision.",
                maxLength = 2000,
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("review_notes")
        @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
        String reviewNotes
) {
}
