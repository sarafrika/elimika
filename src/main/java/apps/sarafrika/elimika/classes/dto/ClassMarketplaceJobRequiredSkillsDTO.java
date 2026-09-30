package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "ClassMarketplaceJobRequiredSkills",
        description = "The skills a marketplace job asks for: its own tags, or its course's when it has none")
public record ClassMarketplaceJobRequiredSkillsDTO(
        @JsonProperty("job_uuid") UUID jobUuid,
        @Schema(description = "True when the job has no tags of its own and these are its course's skills")
        @JsonProperty("inherited") boolean inherited,
        @Schema(description = "The course the skills were inherited from; null when not inherited")
        @JsonProperty("inherited_from_course_uuid") UUID inheritedFromCourseUuid,
        @JsonProperty("skills") List<Skill> skills
) {

    @Schema(name = "ClassMarketplaceJobRequiredSkill")
    public record Skill(
            @JsonProperty("skill_uuid") UUID skillUuid,
            @JsonProperty("skill_name") String skillName,
            @JsonProperty("skill_slug") String skillSlug,
            @Schema(description = "Minimum proficiency asked; for an inherited skill, the level the course teaches")
            @JsonProperty("min_proficiency") ProficiencyLevel minProficiency,
            @Schema(description = "Inherited skills are all mandatory")
            @JsonProperty("is_mandatory") boolean isMandatory,
            @JsonProperty("inherited") boolean inherited,
            @Schema(description = "False when an admin has since retired the skill")
            @JsonProperty("skill_active") boolean skillActive
    ) {
    }
}
