package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "StudentGroup", description = "An organisation-scoped named collection of students.")
public record StudentGroupDTO(

        @Schema(description = "Unique identifier.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Owning organisation UUID.")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Group name.")
        @JsonProperty("name")
        String name,

        @Schema(description = "Optional description.", nullable = true)
        @JsonProperty("description")
        String description,

        @Schema(description = "Optional free-form group type / stream label.", nullable = true)
        @JsonProperty("group_type")
        String groupType,

        @Schema(description = "Number of students in the group.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "member_count", access = JsonProperty.Access.READ_ONLY)
        long memberCount,

        @Schema(description = "When the group was created.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
