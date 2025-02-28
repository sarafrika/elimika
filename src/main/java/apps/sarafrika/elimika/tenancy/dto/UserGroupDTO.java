package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserGroupDTO(
        @JsonProperty("uuid")
        UUID uuid,

        @JsonProperty("organisation_uuid")
        UUID organisationId,

        @NotBlank(message = "Group name is required")
        @JsonProperty("name")
        String name,

        @NotNull(message = "Active status is required")
        @JsonProperty("active")
        Boolean active,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "modified_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime modifiedDate
) {
}
