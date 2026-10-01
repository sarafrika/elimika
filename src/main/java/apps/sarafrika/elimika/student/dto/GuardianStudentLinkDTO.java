package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "GuardianStudentLink", description = "Represents a guardian's access rights to a learner profile.")
public record GuardianStudentLinkDTO(
        @JsonProperty("uuid")
        UUID uuid,
        @JsonProperty("student_uuid")
        UUID studentUuid,
        @JsonProperty("guardian_user_uuid")
        UUID guardianUserUuid,
        @JsonProperty("student_name")
        String studentName,
        @JsonProperty("guardian_display_name")
        String guardianDisplayName,
        @JsonProperty("relationship_type")
        GuardianRelationshipType relationshipType,
        @JsonProperty("share_scope")
        GuardianShareScope shareScope,
        @JsonProperty("status")
        GuardianLinkStatus status,
        @JsonProperty("primary_guardian")
        boolean primaryGuardian,
        @JsonProperty("linked_date")
        LocalDateTime linkedDate,
        @JsonProperty("revoked_date")
        LocalDateTime revokedDate,
        @JsonProperty("notes")
        String notes
) {
}
