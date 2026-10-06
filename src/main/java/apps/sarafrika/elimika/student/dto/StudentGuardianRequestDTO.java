package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.shared.utils.validation.ValidPhoneNumber;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A guardian named by the student; the email decides whether they are linked at once or invited. */
@Schema(name = "StudentGuardianRequest", description = "A parent or guardian named during student onboarding.")
public record StudentGuardianRequestDTO(

        @Schema(description = "**[REQUIRED]** Guardian's full name.", example = "Mary Doe",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian name is required")
        @Size(max = 100, message = "Guardian name cannot exceed 100 characters")
        @JsonProperty("name")
        String name,

        @Schema(description = "**[REQUIRED]** Guardian's email. An existing account is linked straight away; "
                + "otherwise an invitation is emailed here.", example = "mary@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Guardian email is required")
        @Email(message = "Guardian email must be valid")
        @Size(max = 150, message = "Guardian email cannot exceed 150 characters")
        @JsonProperty("email")
        String email,

        @Schema(description = "**[OPTIONAL]** Guardian's mobile number, including country code.",
                example = "+254712345678", nullable = true)
        @Size(max = 20, message = "Guardian phone cannot exceed 20 characters")
        @ValidPhoneNumber(mobileOnly = true)
        @JsonProperty("phone")
        String phone,

        @Schema(description = "**[OPTIONAL]** Relationship to the student. Defaults to GUARDIAN.",
                example = "PARENT", allowableValues = {"PARENT", "GUARDIAN", "SPONSOR"}, nullable = true)
        @Pattern(regexp = "(?i)PARENT|GUARDIAN|SPONSOR",
                message = "Relationship must be one of PARENT, GUARDIAN or SPONSOR")
        @JsonProperty("relationship_type")
        String relationshipType
) {
}
