package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** An open guardian invitation addressed to the signed-in user's email. */
@Schema(name = "MyStudentGuardianInvitation", description = "An open invitation to become a student's guardian.")
public record MyStudentGuardianInvitationDTO(

        @Schema(description = "Invitation id, used to accept or decline")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "The student who named you", example = "Sam Doe")
        @JsonProperty("student_name")
        String studentName,

        @Schema(description = "Relationship the student declared", example = "PARENT")
        @JsonProperty("relationship_type")
        GuardianRelationshipType relationshipType,

        @Schema(description = "When the invitation lapses")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt
) {
}
