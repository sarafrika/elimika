package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An invitation as it appears in the recipient's own inbox.
 * <p>
 * Lets someone who never opened the email still find and act on the offer, so the emailed
 * link is a convenience rather than the only way in.
 */
@Schema(
        name = "MyInvitation",
        description = "An open invitation addressed to the authenticated user."
)
public record MyInvitationDTO(

        @Schema(description = "Invitation identifier, used to accept or decline from the inbox", format = "uuid")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Name of the inviting organisation", example = "Sarafrika Academy")
        @JsonProperty("organisation_name")
        String organisationName,

        @Schema(description = "Organisation identifier", format = "uuid")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Who sent the invitation", nullable = true)
        @JsonProperty("inviter_name")
        String inviterName,

        @Schema(description = "Role being offered", example = "student")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(description = "Personal note from the organisation", nullable = true)
        @JsonProperty("message")
        String message,

        @Schema(description = "Number of classes that will be surfaced on acceptance", example = "3")
        @JsonProperty("class_count")
        int classCount,

        @Schema(description = "Current lifecycle state", example = "PENDING")
        @JsonProperty("status")
        InvitationStatus status,

        @Schema(description = "Whether the offer is waiting on a guardian rather than on the recipient")
        @JsonProperty("requires_guardian_consent")
        boolean requiresGuardianConsent,

        @Schema(description = "When the invitation lapses")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt,

        @Schema(description = "When the invitation was sent")
        @JsonProperty("created_date")
        LocalDateTime createdDate
) {
}
