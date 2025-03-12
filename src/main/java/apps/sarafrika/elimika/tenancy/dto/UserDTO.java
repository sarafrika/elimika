package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserDTO (

        @JsonProperty("uuid")
        UUID uuid,

        @NotBlank(message = "First name is required")
        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("middle_name")
        String middleName,

        @NotBlank(message = "Last name is required")
        @JsonProperty("last_name")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @JsonProperty("email")
        String email,

        @NotBlank(message = "Phone number is required")
        @JsonProperty("phone_number")
        String phoneNumber,

        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @JsonProperty("active")
        boolean active,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "modified_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime modifiedDate,

        @JsonProperty("roles")
        Set<RoleDTO> roles
){
}
