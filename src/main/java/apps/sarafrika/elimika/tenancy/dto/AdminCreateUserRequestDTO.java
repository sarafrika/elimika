package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new admin user.
 */
public record AdminCreateUserRequestDTO(

        @Schema(description = "First name of the admin user", example = "Jane")
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        @JsonProperty("first_name")
        String firstName,

        @Schema(description = "Middle name of the admin user", example = "A.")
        @Size(max = 100, message = "Middle name cannot exceed 100 characters")
        @JsonProperty("middle_name")
        String middleName,

        @Schema(description = "Last name of the admin user", example = "Doe")
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        @JsonProperty("last_name")
        String lastName,

        @Schema(description = "Email address of the admin user", example = "jane.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        @JsonProperty("email")
        String email,

        @Schema(description = "Optional phone number", example = "+254700000000")
        @Size(max = 50, message = "Phone number cannot exceed 50 characters")
        @JsonProperty("phone_number")
        String phoneNumber
) {
}
