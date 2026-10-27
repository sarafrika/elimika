package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "ProgramTargetSkill", description = "A skill the programme develops, with the graded evidence linked to it")
public record ProgramTargetSkillDTO(
        @JsonProperty("skill_uuid") UUID skillUuid,
        @JsonProperty("skill_name") String skillName,
        @JsonProperty("display_order") Integer displayOrder,
        @Schema(description = "False until at least one graded source is linked; the programme can't be published then")
        @JsonProperty("ready") boolean ready,
        @JsonProperty("mappings") List<ProgramSkillMappingDTO> mappings
) {
}
