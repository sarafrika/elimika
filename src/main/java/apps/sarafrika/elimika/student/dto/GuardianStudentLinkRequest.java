package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "GuardianStudentLinkRequest", description = "Request payload to link a guardian/parent to a learner profile.")
public record GuardianStudentLinkRequest(

        @Schema(description = "UUID for the student profile to be monitored", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID studentUuid,

        @Schema(description = "UUID for the guardian's user account", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UUID guardianUserUuid,

        @Schema(description = "Nature of the relationship between the guardian and the learner",
                defaultValue = "PARENT")
        @NotNull
        GuardianRelationshipType relationshipType,

        @Schema(description = "Access scope granted to the guardian", defaultValue = "FULL")
        @NotNull
        GuardianShareScope shareScope,

        @Schema(description = "Marks this guardian as the primary contact", defaultValue = "false")
        boolean isPrimary,

        @Schema(description = "Optional note shown in audits or invitation emails")
        String notes
) {
}
