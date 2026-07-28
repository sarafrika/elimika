package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outcome of a batch send.
 * <p>
 * A partial failure is normal - one bad address should not cost the sender the whole
 * batch - so every recipient is reported individually.
 */
@Schema(
        name = "SendOrganisationInvitationsResult",
        description = "Per-recipient outcome of a batch invitation send."
)
public record SendOrganisationInvitationsResultDTO(

        @Schema(description = "Invitations that were created and queued for delivery")
        @JsonProperty("sent")
        List<OrganisationInvitationDTO> sent,

        @Schema(description = "Recipients that could not be invited, with the reason why")
        @JsonProperty("failed")
        List<Failure> failed
) {

    @Schema(name = "OrganisationInvitationFailure", description = "A recipient that could not be invited.")
    public record Failure(

            @Schema(description = "Email address that could not be invited", example = "jane.doe@example.com")
            @JsonProperty("email")
            String email,

            @Schema(description = "Why the invitation was not created",
                    example = "This address already has a pending invitation to your organisation.")
            @JsonProperty("reason")
            String reason
    ) {
    }
}
