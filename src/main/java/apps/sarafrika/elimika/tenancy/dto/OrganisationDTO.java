package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganisationDTO (
        @JsonProperty("uuid")
        UUID uuid,

        @NotBlank(message = "Organisation name is required")
        @Size(max = 50, message = "Organisation name cannot exceed 50 characters")
        @JsonProperty("name")
        String name,

        @JsonProperty("description")
        String description,

        @JsonProperty("active")
        boolean active,

        @JsonProperty("code")
        String code,

        @JsonProperty(value = "slug", access = JsonProperty.Access.READ_ONLY)
        String slug,

        @JsonProperty("domain")
        @NotBlank(message = "Domain is required")
        String domain,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdAt,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedAt
) {
}
