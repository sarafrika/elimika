package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "CourseSkill", description = "A skill the course teaches, from the skills taxonomy")
public record CourseSkillDTO(
        @JsonProperty("skill_uuid") UUID skillUuid,
        @JsonProperty("skill_name") String skillName,
        @JsonProperty("skill_slug") String skillSlug,
        @Schema(description = "Level the course teaches the skill to", allowableValues = {"beginner", "intermediate", "advanced", "expert"})
        @JsonProperty("level") ProficiencyLevel level,
        @Schema(description = "1-5, how central the skill is to the course")
        @JsonProperty("weight") int weight,
        @Schema(description = "False when an admin has since retired the skill; the tag stays until the owner removes it")
        @JsonProperty("skill_active") boolean skillActive
) {
}
