package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What an invitation link reveals before anyone signs in.
 * <p>
 * Anyone holding the link can read this, so it carries only what the recipient needs in
 * order to decide: who is inviting them, into what role, and until when. The recipient's
 * email is masked, and no other personal data appears.
 */
@Schema(
        name = "PublicInvitation",
        description = "The publicly readable view of an invitation link."
)
public record PublicInvitationDTO(

        @Schema(description = "Name of the inviting organisation", example = "Sarafrika Academy")
        @JsonProperty("organisation_name")
        String organisationName,

        @Schema(description = "Organisation identifier", format = "uuid")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Who sent the invitation", example = "Jane Doe", nullable = true)
        @JsonProperty("inviter_name")
        String inviterName,

        @Schema(description = "Role the recipient is invited into", example = "student")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(description = "Masked recipient address, so the recipient can confirm the link is for them",
                example = "j***e@example.com")
        @JsonProperty("masked_recipient_email")
        String maskedRecipientEmail,

        @Schema(description = "Display name the organisation supplied for the recipient", nullable = true)
        @JsonProperty("recipient_name")
        String recipientName,

        @Schema(description = "Whether this address already has an Elimika account. Drives whether the " +
                "page offers sign-in or registration.")
        @JsonProperty("existing_platform_user")
        boolean existingPlatformUser,

        @Schema(description = "Personal note from the inviting organisation", nullable = true)
        @JsonProperty("message")
        String message,

        @Schema(description = "Number of classes that will be surfaced on acceptance", example = "3")
        @JsonProperty("class_count")
        int classCount,

        @Schema(description = "Current lifecycle state", example = "PENDING")
        @JsonProperty("status")
        InvitationStatus status,

        @Schema(description = "Whether the offer can still be acted upon")
        @JsonProperty("actionable")
        boolean actionable,

        @Schema(description = "Whether the offer is waiting on a guardian rather than the invitee")
        @JsonProperty("requires_guardian_consent")
        boolean requiresGuardianConsent,

        @Schema(description = "When the invitation lapses")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt
) {
}
