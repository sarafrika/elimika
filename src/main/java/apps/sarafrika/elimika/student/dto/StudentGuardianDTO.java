package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.StudentGuardianState;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** One of a student's guardians and where their access stands. */
@Schema(name = "StudentGuardian", description = "A student's guardian with link or invitation status.")
public record StudentGuardianDTO(

        @Schema(description = "Guardian entry id, used to resend an invitation. Null for a guardian linked "
                + "outside onboarding (for example through organisation consent).", nullable = true)
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "The student profile")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(description = "Guardian's name", example = "Mary Doe")
        @JsonProperty("name")
        String name,

        @Schema(description = "Guardian's email", example = "mary@example.com")
        @JsonProperty("email")
        String email,

        @Schema(description = "Guardian's mobile number", nullable = true)
        @JsonProperty("phone")
        String phone,

        @Schema(description = "Relationship to the student", example = "PARENT")
        @JsonProperty("relationship_type")
        GuardianRelationshipType relationshipType,

        @Schema(description = "linked, invited, expired, declined or revoked", example = "invited")
        @JsonProperty("status")
        StudentGuardianState status,

        @Schema(description = "Guardian's account, once known", nullable = true)
        @JsonProperty("guardian_user_uuid")
        UUID guardianUserUuid,

        @Schema(description = "The guardian link granting access, once linked", nullable = true)
        @JsonProperty("link_uuid")
        UUID linkUuid,

        @Schema(description = "When the latest invitation was emailed", nullable = true)
        @JsonProperty("invitation_sent_at")
        LocalDateTime invitationSentAt,

        @Schema(description = "When the latest invitation lapses", nullable = true)
        @JsonProperty("invitation_expires_at")
        LocalDateTime invitationExpiresAt,

        @Schema(description = "When access was granted", nullable = true)
        @JsonProperty("linked_at")
        LocalDateTime linkedAt
) {
}
