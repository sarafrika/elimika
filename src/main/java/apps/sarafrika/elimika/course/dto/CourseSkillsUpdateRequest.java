package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(name = "CourseSkillsUpdateRequest",
        description = "The course's complete skill tag list; it replaces the current one. An empty list clears the tags.")
public record CourseSkillsUpdateRequest(
        @NotNull(message = "skills is required (send [] to clear)")
        @Size(max = 30, message = "a course may carry at most 30 skills")
        @Valid
        @JsonProperty("skills") List<Item> skills
) {

    @Schema(name = "CourseSkillItem")
    public record Item(
            @NotNull(message = "skill_uuid is required")
            @JsonProperty("skill_uuid") UUID skillUuid,

            @Schema(description = "Defaults to beginner; accepted in any case", allowableValues = {"beginner", "intermediate", "advanced", "expert"})
            @JsonProperty("level") ProficiencyLevel level,

            @Schema(description = "1-5, defaults to 1")
            @Min(value = 1, message = "weight must be between 1 and 5")
            @Max(value = 5, message = "weight must be between 1 and 5")
            @JsonProperty("weight") Integer weight
    ) {
    }
}
