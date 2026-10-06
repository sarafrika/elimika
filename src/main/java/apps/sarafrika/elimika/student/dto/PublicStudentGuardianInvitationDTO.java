package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.StudentGuardianState;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** What a guardian invitation link reveals before the guardian signs in. */
@Schema(name = "PublicStudentGuardianInvitation", description = "Publicly readable view of a guardian invitation link.")
public record PublicStudentGuardianInvitationDTO(

        @Schema(description = "The student who named the guardian", example = "Sam Doe")
        @JsonProperty("student_name")
        String studentName,

        @Schema(description = "Guardian's name as the student gave it", example = "Mary Doe")
        @JsonProperty("guardian_name")
        String guardianName,

        @Schema(description = "Masked invited address, to confirm it is theirs", example = "m***y@example.com")
        @JsonProperty("masked_guardian_email")
        String maskedGuardianEmail,

        @Schema(description = "Relationship the student declared", example = "PARENT")
        @JsonProperty("relationship_type")
        GuardianRelationshipType relationshipType,

        @Schema(description = "invited, expired, linked, declined or revoked", example = "invited")
        @JsonProperty("status")
        StudentGuardianState status,

        @Schema(description = "Whether the invitation can still be accepted or declined")
        @JsonProperty("actionable")
        boolean actionable,

        @Schema(description = "Whether the invited address already has an account: sign in to accept, else register")
        @JsonProperty("has_account")
        boolean hasAccount,

        @Schema(description = "When the invitation lapses")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt
) {
}
