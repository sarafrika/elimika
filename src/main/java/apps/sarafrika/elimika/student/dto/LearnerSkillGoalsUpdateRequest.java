package apps.sarafrika.elimika.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** The complete list of a learner's skill goals; an empty list clears them. */
@Schema(name = "LearnerSkillGoalsUpdateRequest", description = "Replaces the learner's skill goals")
public record LearnerSkillGoalsUpdateRequest(
        @Schema(description = "Skill uuids from GET /api/v1/skills, at most 20")
        @JsonProperty("skill_uuids")
        @NotNull
        @Size(max = LearnerSkillGoalsUpdateRequest.MAX_GOALS)
        List<UUID> skillUuids
) {
    public static final int MAX_GOALS = 20;
}
