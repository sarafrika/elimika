package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Fields are snake_case. The camelCase names the endpoint used to take are still accepted on input
 * through {@link JsonAlias} for one release, so deployed clients keep working while they move over.
 */
@Schema(name = "GuardianStudentLinkRequest", description = "Request payload to link a guardian/parent to a learner profile.")
public record GuardianStudentLinkRequest(

        @Schema(description = "UUID for the student profile to be monitored", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @JsonProperty("student_uuid")
        @JsonAlias("studentUuid")
        UUID studentUuid,

        @Schema(description = "UUID for the guardian's user account", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @JsonProperty("guardian_user_uuid")
        @JsonAlias("guardianUserUuid")
        UUID guardianUserUuid,

        @Schema(description = "Nature of the relationship between the guardian and the learner",
                defaultValue = "PARENT")
        @NotNull
        @JsonProperty("relationship_type")
        @JsonAlias("relationshipType")
        GuardianRelationshipType relationshipType,

        @Schema(description = "Access scope granted to the guardian", defaultValue = "FULL")
        @NotNull
        @JsonProperty("share_scope")
        @JsonAlias("shareScope")
        GuardianShareScope shareScope,

        @Schema(description = "Marks this guardian as the primary contact", defaultValue = "false")
        @JsonProperty("is_primary")
        @JsonAlias("isPrimary")
        boolean isPrimary,

        @Schema(description = "Optional note shown in audits or invitation emails")
        @JsonProperty("notes")
        String notes
) {
}
