package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoleDTO(
        @JsonProperty("uuid")
        UUID uuid,

        @NotNull(message = "Organisation UUID is required")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @NotBlank(message = "Role name is required")
        @JsonProperty("name")
        String name,

        @JsonProperty("description")
        String description,

        @JsonProperty("active")
        boolean active,

        List<PermissionDTO> permissions,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "modified_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime modifiedDate
) {
}
