package apps.sarafrika.elimika.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Withdraws an obligation that should never have accrued — a session voided after it was marked
 * complete, for instance.
 * <p>
 * A reason is mandatory because the row is not deleted: it stays, excluded from what is owed, saying
 * why. Deleting it would hide the correction instead of recording it.
 */
@Schema(
        name = "InstructorObligationCancellationRequest",
        description = "Reason for withdrawing an obligation that should never have accrued"
)
public record InstructorObligationCancellationRequestDTO(

        @Schema(
                description = "Why this obligation is being withdrawn",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Session was marked complete in error and has been rescheduled"
        )
        @JsonProperty("reason")
        @NotBlank(message = "A cancellation reason is required")
        String reason

) {
}
