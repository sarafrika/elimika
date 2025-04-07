package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ProfessionalBodyDTO(

        @Schema(description = "Name of the professional body", example = "Kenya Medical Association")
        @NotBlank(message = "Professional body name is required")
        @Size(max = 100, message = "Professional body name cannot exceed 100 characters")
        @JsonProperty("body_name")
        String bodyName,

        @Schema(description = "Membership number assigned by the body", example = "KM123456")
        @NotBlank(message = "Membership number is required")
        @Size(max = 50, message = "Membership number cannot exceed 50 characters")
        @JsonProperty("membership_no")
        String membershipNo,

        @Schema(description = "Date when the user became a member (ISO 8601 format)", example = "2020-06-15")
        @NotBlank(message = "Membership start date is required")
        @JsonProperty("member_since")
        LocalDate memberSince,

        @Schema(description = "UUID of the user associated with this membership", example = "f4b4e0c2-8c7b-4d2b-b2a1-198c7a7f3df0")
        @NotBlank(message = "User UUID is required")
        @JsonProperty("user_uuid")
        UUID userUuid

) {
}
