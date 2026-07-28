package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * Payload for accepting an invitation.
 * <p>
 * The date of birth decides whether the invitee may consent for themselves or whether the
 * offer has to go to a guardian. It is used for that decision and stored on the user's own
 * profile - it is never exposed to the inviting organisation.
 */
@Schema(
        name = "AcceptInvitationRequest",
        description = "Accepts an invitation, acknowledging what the organisation will be able to see."
)
public record AcceptInvitationRequestDTO(

        @Schema(description = "**[OPTIONAL]** Date of birth. Required only when the account does not " +
                "already have one on file.", example = "2004-05-17", nullable = true)
        @Past(message = "Date of birth must be in the past")
        @JsonProperty("date_of_birth")
        LocalDate dateOfBirth,

        @Schema(description = "**[REQUIRED]** Confirms the invitee has seen what the organisation will " +
                "be able to view: their enrolment and performance in that institution's own classes, " +
                "and nothing beyond it.",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @AssertTrue(message = "The data-sharing scope must be acknowledged before accepting")
        @JsonProperty("scope_acknowledged")
        boolean scopeAcknowledged
) {
}
