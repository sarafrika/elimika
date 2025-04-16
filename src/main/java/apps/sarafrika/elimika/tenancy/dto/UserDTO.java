package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserDTO(

        @Schema(description = "Unique identifier of the user", example = "d2e6f6c4-3d44-11ee-be56-0242ac120002")
        @NotNull(message = "UUID is required")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "User's first name", example = "Jane")
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        @JsonProperty("first_name")
        String firstName,

        @Schema(description = "User's middle name (Optional)", example = "A.", nullable = true)
        @Size(max = 50, message = "Middle name must not exceed 50 characters")
        @JsonProperty("middle_name")
        String middleName,

        @Schema(description = "User's last name", example = "Doe")
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        @JsonProperty("last_name")
        String lastName,

        @Schema(description = "User's email address", example = "jane.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        @JsonProperty("email")
        String email,

        @Schema(description = "User's phone number", example = "+254712345678")
        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        @JsonProperty("phone_number")
        String phoneNumber,

        @Schema(description = "URL of the user's profile image", example = "https://example.com/images/jane.jpg", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "profile_image_url", access = JsonProperty.Access.READ_ONLY)
        String profileImageUrl,

        @Schema(description = "User's date of birth", example = "1990-01-01")
        @NotNull(message = "Date of birth is required")
        @JsonProperty("dob")
        LocalDate dob,

        @Schema(description = "Username used for login", example = "janedoe")
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        @JsonProperty("username")
        String username,

        @Schema(description = "UUID of the organisation the user belongs to", example = "b1c2d3e4-f5g6-h7i8-j9k0-lmnopqrstuv")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Indicates if the user is active", example = "true")
        @JsonProperty("active")
        boolean active,

        @Schema(description = "Creation timestamp", example = "2024-04-01T12:00:00", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(description = "Last modified timestamp", example = "2024-04-15T15:30:00", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "modified_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime modifiedDate,

        @Schema(description = "Roles assigned to the user", example = "[\"ADMIN\", \"USER\"]")
        @JsonProperty("roles")
        Set<RoleDTO> roles

) {
}
