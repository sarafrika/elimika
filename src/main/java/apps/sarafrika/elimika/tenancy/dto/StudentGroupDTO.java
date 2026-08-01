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

        @Schema(description = "Stream label within the branch and tier, e.g. \"Stream A\".", nullable = true)
        @JsonProperty("group_type")
        String groupType,

        @Schema(description = "Training branch (campus) running the group. Null for unassigned legacy groups.",
                nullable = true)
        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @Schema(description = "Name of the branch, denormalised so the group list needs no second fetch.",
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "branch_name", access = JsonProperty.Access.READ_ONLY)
        String branchName,

        @Schema(description = "Academic tier (schooling level). Null for unassigned legacy groups.", nullable = true)
        @JsonProperty("tier_uuid")
        UUID tierUuid,

        @Schema(description = "Name of the academic tier, denormalised for filter pills.",
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "tier", access = JsonProperty.Access.READ_ONLY)
        String tier,

        @Schema(description = "Sort position of the academic tier, denormalised so pills sort without a second fetch.",
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "tier_order", access = JsonProperty.Access.READ_ONLY)
        Integer tierOrder,

        @Schema(description = "Intended size of the group. Advisory: enrolment above it is reported, not blocked.",
                nullable = true)
        @JsonProperty("capacity")
        Integer capacity,

        @Schema(description = "Number of students in the group.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "member_count", access = JsonProperty.Access.READ_ONLY)
        long memberCount,

        @Schema(description = "When the group was created.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
