package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(name = "ClassMarketplaceJobRequiredSkillsRequest",
        description = "The job's complete required-skill list; it replaces the current one. [] clears it, and the job inherits its course's skills again.")
public record ClassMarketplaceJobRequiredSkillsRequest(
        @NotNull(message = "skills is required (send [] to clear)")
        @Size(max = 30, message = "a job may require at most 30 skills")
        @Valid
        @JsonProperty("skills") List<Item> skills
) {

    @Schema(name = "ClassMarketplaceJobRequiredSkillItem")
    public record Item(
            @NotNull(message = "skill_uuid is required")
            @JsonProperty("skill_uuid") UUID skillUuid,

            @Schema(description = "Defaults to beginner; accepted in any case", allowableValues = {"beginner", "intermediate", "advanced", "expert"})
            @JsonProperty("min_proficiency") ProficiencyLevel minProficiency,

            @Schema(description = "Defaults to true")
            @JsonProperty("is_mandatory") Boolean isMandatory
    ) {
    }
}
