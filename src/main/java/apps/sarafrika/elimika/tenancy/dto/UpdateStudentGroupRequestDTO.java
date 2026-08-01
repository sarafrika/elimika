package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Full replacement of a group's editable attributes.
 * <p>
 * Omitted optional fields are cleared, matching the sibling {@code PUT /training-branches/{uuid}}.
 * The organisation is not settable: a group cannot be moved between tenants.
 */
@Schema(name = "UpdateStudentGroupRequest",
        description = "Payload to replace an organisation student group's editable attributes.")
public record UpdateStudentGroupRequestDTO(

        @Schema(description = "Group name.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("name")
        @NotBlank(message = "Group name is required")
        String name,

        @Schema(description = "Optional description. Omit to clear.", nullable = true)
        @JsonProperty("description")
        String description,

        @Schema(description = "Stream label within the branch and tier. Omit to clear.", nullable = true)
        @JsonProperty("group_type")
        String groupType,

        @Schema(description = "Training branch running the group. Must belong to the same organisation.",
                nullable = true)
        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @Schema(description = "Academic tier (schooling level) the group sits at.", nullable = true)
        @JsonProperty("tier_uuid")
        UUID tierUuid,

        @Schema(description = "Intended size of the group. Advisory only.", nullable = true)
        @JsonProperty("capacity")
        @Positive(message = "Capacity must be greater than zero")
        Integer capacity
) {
}
