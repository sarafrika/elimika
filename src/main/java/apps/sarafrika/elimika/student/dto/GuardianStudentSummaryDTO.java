package apps.sarafrika.elimika.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;

import java.util.UUID;

public record GuardianStudentSummaryDTO(
        @JsonProperty("link_uuid") UUID linkUuid,
        @JsonProperty("student_uuid") UUID studentUuid,
        @JsonProperty("student_name") String studentName,
        @JsonProperty("relationship_type") GuardianRelationshipType relationshipType,
        @JsonProperty("share_scope") GuardianShareScope shareScope,
        @JsonProperty("status") GuardianLinkStatus status,
        @JsonProperty("primary_guardian") boolean primaryGuardian
) {
}
