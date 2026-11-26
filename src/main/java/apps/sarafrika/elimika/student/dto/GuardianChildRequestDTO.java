package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** A child a signed-in guardian adds as a learner they manage; the child gets no login. */
@Schema(name = "GuardianChildRequest",
        description = "A child the signed-in guardian adds as a learner. The child gets a learner profile but no "
                + "login, and the guardian is linked to it with full access.")
public record GuardianChildRequestDTO(

        @Schema(description = "**[REQUIRED]** Child's first name", example = "Wanjiru",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        @JsonProperty("first_name")
        String firstName,

        @Schema(description = "**[OPTIONAL]** Child's middle name", example = "Njeri", nullable = true)
        @Size(max = 50, message = "Middle name cannot exceed 50 characters")
        @JsonProperty("middle_name")
        String middleName,

        @Schema(description = "**[REQUIRED]** Child's last name", example = "Kamau",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        @JsonProperty("last_name")
        String lastName,

        @Schema(description = "**[REQUIRED]** Child's date of birth. The child must be under 18, and the learner "
                + "age gate applies.", example = "2015-08-21", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        @JsonProperty("dob")
        LocalDate dob,

        @Schema(description = "**[REQUIRED]** How the guardian relates to the child: `PARENT` or `GUARDIAN`.",
                example = "PARENT", allowableValues = {"PARENT", "GUARDIAN"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Relationship is required")
        @JsonProperty("relationship_type")
        GuardianRelationshipType relationshipType
) {
}
