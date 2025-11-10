package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "GuardianStudentLink", description = "Represents a guardian's access rights to a learner profile.")
public record GuardianStudentLinkDTO(
        UUID uuid,
        UUID studentUuid,
        UUID guardianUserUuid,
        String studentName,
        String guardianDisplayName,
        GuardianRelationshipType relationshipType,
        GuardianShareScope shareScope,
        GuardianLinkStatus status,
        boolean primaryGuardian,
        LocalDateTime linkedDate,
        LocalDateTime revokedDate,
        String notes
) {
}
