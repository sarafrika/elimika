package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(name = "AddGroupMembersRequest", description = "Payload to add one or more students to a group.")
public record AddGroupMembersRequestDTO(

        @Schema(description = "Student user UUIDs to add.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("student_uuids")
        @NotEmpty(message = "At least one student UUID is required")
        List<UUID> studentUuids
) {
}
