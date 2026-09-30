package apps.sarafrika.elimika.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** One skill a learner wants to learn, from the skills taxonomy. */
@Schema(name = "LearnerSkillGoal", description = "A skill the learner has declared as a learning goal")
public record LearnerSkillGoalDTO(
        @Schema(description = "Skill uuid from the skills taxonomy")
        @JsonProperty("skill_uuid") UUID skillUuid,

        @Schema(description = "Skill name")
        @JsonProperty("name") String name,

        @Schema(description = "Skill slug")
        @JsonProperty("slug") String slug,

        @Schema(description = "Who declared the goal: self, guardian or admin", example = "self")
        @JsonProperty("source") String source,

        @Schema(description = "When the goal was declared (UTC)")
        @JsonProperty("created_date") LocalDateTime createdDate
) {
}
