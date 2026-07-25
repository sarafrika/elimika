package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateStudentGroupRequest", description = "Payload to create an organisation student group.")
public record CreateStudentGroupRequestDTO(

        @Schema(description = "Group name.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("name")
        @NotBlank(message = "Group name is required")
        String name,

        @Schema(description = "Optional description.", nullable = true)
        @JsonProperty("description")
        String description,

        @Schema(description = "Optional free-form group type / stream label.", nullable = true)
        @JsonProperty("group_type")
        String groupType
) {
}
