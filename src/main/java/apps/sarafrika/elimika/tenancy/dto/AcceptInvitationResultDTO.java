package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of accepting an invitation.
 * <p>
 * Acceptance does not always complete the journey: a minor's acceptance hands the
 * decision to their guardian instead, and no affiliation exists until that guardian
 * consents. {@code affiliated} says which of the two happened.
 */
@Schema(
        name = "AcceptInvitationResult",
        description = "What happened when an invitation was accepted."
)
public record AcceptInvitationResultDTO(

        @Schema(description = "Resulting lifecycle state", example = "ACCEPTED")
        @JsonProperty("status")
        InvitationStatus status,

        @Schema(description = "True when the affiliation now exists. False when the offer has been " +
                "passed to a guardian for consent.")
        @JsonProperty("affiliated")
        boolean affiliated,

        @Schema(description = "True when the invitee was found to be a minor and a guardian must consent")
        @JsonProperty("guardian_consent_required")
        boolean guardianConsentRequired,

        @Schema(description = "Organisation joined, or that consent is being sought for", format = "uuid")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Name of that organisation")
        @JsonProperty("organisation_name")
        String organisationName,

        @Schema(description = "Classes now surfaced to the student. These are recommendations - " +
                "the student still enrols in each one separately.")
        @JsonProperty("surfaced_class_uuids")
        List<UUID> surfacedClassUuids,

        @Schema(description = "What the caller should do next, in plain language")
        @JsonProperty("message")
        String message
) {
}
