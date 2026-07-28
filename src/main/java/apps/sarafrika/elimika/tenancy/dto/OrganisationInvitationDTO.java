package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Organisation-facing view of an invitation.
 * <p>
 * Deliberately excludes both tokens and the invitee's date of birth. For a minor, the
 * organisation sees only that guardian consent is pending or granted - never the birth
 * date, and never guardian contact details beyond what it supplied itself.
 */
@Schema(
        name = "OrganisationInvitation",
        description = "An invitation to join an organisation, as seen by the inviting organisation."
)
public record OrganisationInvitationDTO(

        @Schema(description = "Invitation identifier", format = "uuid")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Organisation that issued the invitation", format = "uuid")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Training branch the invitation is scoped to, if any", format = "uuid", nullable = true)
        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @Schema(description = "Org-scoped domain the recipient was invited into", example = "student")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(description = "Email address invited", example = "jane.doe@example.com")
        @JsonProperty("recipient_email")
        String recipientEmail,

        @Schema(description = "Display name of the invitee", nullable = true)
        @JsonProperty("recipient_name")
        String recipientName,

        @Schema(description = "Set when the invited email already belonged to a platform user",
                format = "uuid", nullable = true)
        @JsonProperty("recipient_user_uuid")
        UUID recipientUserUuid,

        @Schema(description = "Whether the invited email already had a platform account when sent")
        @JsonProperty("existing_platform_user")
        boolean existingPlatformUser,

        @Schema(description = "Who sent the invitation", format = "uuid")
        @JsonProperty("inviter_user_uuid")
        UUID inviterUserUuid,

        @Schema(description = "Current lifecycle state", example = "PENDING")
        @JsonProperty("status")
        InvitationStatus status,

        @Schema(description = "Personal note included in the invitation email", nullable = true)
        @JsonProperty("message")
        String message,

        @Schema(description = "Classes surfaced to the recipient on acceptance")
        @JsonProperty("class_uuids")
        List<UUID> classUuids,

        @Schema(description = "When the invitation lapses")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt,

        @Schema(description = "When the invitation was accepted", nullable = true)
        @JsonProperty("accepted_at")
        LocalDateTime acceptedAt,

        @Schema(description = "When the invitation was declined", nullable = true)
        @JsonProperty("declined_at")
        LocalDateTime declinedAt,

        @Schema(description = "When the organisation withdrew the invitation", nullable = true)
        @JsonProperty("revoked_at")
        LocalDateTime revokedAt,

        @Schema(description = "True when the invitee declared a date of birth below the age gate, " +
                "so the offer awaits guardian consent. The date of birth itself is never exposed.")
        @JsonProperty("requires_guardian_consent")
        boolean requiresGuardianConsent,

        @Schema(description = "When the guardian consented", nullable = true)
        @JsonProperty("guardian_consented_at")
        LocalDateTime guardianConsentedAt,

        @Schema(description = "When the invitation was created")
        @JsonProperty("created_date")
        LocalDateTime createdDate
) {
}
