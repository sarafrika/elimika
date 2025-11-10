package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;

import java.util.UUID;

public record GuardianStudentSummaryDTO(
        UUID linkUuid,
        UUID studentUuid,
        String studentName,
        GuardianRelationshipType relationshipType,
        GuardianShareScope shareScope,
        GuardianLinkStatus status,
        boolean primaryGuardian
) {
}
