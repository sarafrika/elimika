package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(name = "ProgramTargetSkillsRequest", description = "The programme's target skills in display order; removed skills lose their links")
public record ProgramTargetSkillsRequest(
        @NotNull
        @JsonProperty("skill_uuids")
        List<UUID> skillUuids
) {
}
