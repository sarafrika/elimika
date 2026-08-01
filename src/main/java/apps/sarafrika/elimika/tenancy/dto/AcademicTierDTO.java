package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "AcademicTier", description = "An ordered schooling level that student groups are filed under.")
public record AcademicTierDTO(

        @Schema(description = "Unique identifier.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Display name of the level, e.g. \"Grade 7\".")
        @JsonProperty("name")
        String name,

        @Schema(description = "Sort position. Gapped by tens so levels can be inserted without renumbering.")
        @JsonProperty("tier_order")
        Integer tierOrder,

        @Schema(description = "Curriculum the level belongs to, e.g. \"KE\".")
        @JsonProperty("education_system")
        String educationSystem,

        @Schema(description = "Owning organisation, or null for the shared platform catalogue.", nullable = true)
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Whether the level is offered in pickers.")
        @JsonProperty("active")
        boolean active,

        @Schema(description = "Optional description.", nullable = true)
        @JsonProperty("description")
        String description
) {
}
