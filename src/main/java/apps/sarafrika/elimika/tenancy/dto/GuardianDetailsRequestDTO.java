package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Guardian details supplied by a minor whose invitation needs adult consent.
 * <p>
 * Deliberately minimal: enough to reach the guardian and to record the nature of their
 * relationship, and nothing more. No identity documents or national identifiers are
 * collected, and none of this is shown back to the inviting organisation.
 */
@Schema(
        name = "GuardianDetailsRequest",
        description = "Contact details for the guardian who will consent on a minor's behalf."
)
public record GuardianDetailsRequestDTO(

        @Schema(description = "**[REQUIRED]** Guardian's email address. The consent link is sent here.",
                example = "parent@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian email is required")
        @Email(message = "Guardian email must be valid")
        @Size(max = 150, message = "Guardian email cannot exceed 150 characters")
        @JsonProperty("guardian_email")
        String guardianEmail,

        @Schema(description = "**[REQUIRED]** Guardian's full name.", example = "Mary Doe",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian name is required")
        @Size(max = 150, message = "Guardian name cannot exceed 150 characters")
        @JsonProperty("guardian_name")
        String guardianName,

        @Schema(description = "**[REQUIRED]** Nature of the relationship.", example = "PARENT",
                allowableValues = {"PARENT", "GUARDIAN", "SPONSOR"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian relationship type is required")
        @Pattern(regexp = "(?i)PARENT|GUARDIAN|SPONSOR",
                message = "Relationship must be one of PARENT, GUARDIAN or SPONSOR")
        @JsonProperty("guardian_relationship_type")
        String guardianRelationshipType,

        @Schema(description = "**[OPTIONAL]** Guardian's phone number, used only if the email bounces.",
                example = "+254700000000", nullable = true)
        @Size(max = 50, message = "Guardian phone cannot exceed 50 characters")
        @JsonProperty("guardian_phone")
        String guardianPhone
) {
}
