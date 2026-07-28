package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What a guardian consent link reveals before the guardian signs in.
 * <p>
 * Names the child so the guardian can confirm the request is genuinely about their own
 * dependant, and names the organisation and role they are being asked to approve.
 */
@Schema(
        name = "PublicGuardianInvitation",
        description = "The publicly readable view of a guardian consent link."
)
public record PublicGuardianInvitationDTO(

        @Schema(description = "Name of the organisation seeking to enrol the child", example = "Sarafrika Academy")
        @JsonProperty("organisation_name")
        String organisationName,

        @Schema(description = "Organisation identifier", format = "uuid")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "The child's display name", example = "Sam Doe", nullable = true)
        @JsonProperty("student_name")
        String studentName,

        @Schema(description = "Masked address of the child, so the guardian can confirm who this concerns",
                example = "s***m@example.com")
        @JsonProperty("masked_student_email")
        String maskedStudentEmail,

        @Schema(description = "Guardian's name as the child supplied it", example = "Mary Doe")
        @JsonProperty("guardian_name")
        String guardianName,

        @Schema(description = "Relationship the child declared", example = "PARENT")
        @JsonProperty("guardian_relationship_type")
        String guardianRelationshipType,

        @Schema(description = "Role the child would take at the organisation", example = "student")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(description = "Number of classes that would be surfaced to the child", example = "3")
        @JsonProperty("class_count")
        int classCount,

        @Schema(description = "Current lifecycle state", example = "AWAITING_GUARDIAN_CONSENT")
        @JsonProperty("status")
        InvitationStatus status,

        @Schema(description = "Whether consent can still be given")
        @JsonProperty("actionable")
        boolean actionable,

        @Schema(description = "When the request lapses")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt
) {
}
