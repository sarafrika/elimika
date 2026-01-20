package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "ClassDefinitionResponse",
        description = "Response payload for class definition operations including schedule and conflicts"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassDefinitionResponseDTO(

        @Schema(description = "Persisted class definition")
        @JsonProperty("class_definition")
        ClassDefinitionDTO classDefinition,

        @Schema(description = "Instances scheduled for the class definition")
        @JsonProperty("scheduled_instances")
        List<ScheduledInstanceDTO> scheduledInstances,

        @Schema(description = "Conflicts encountered while scheduling")
        @JsonProperty("scheduling_conflicts")
        List<ClassSchedulingConflictDTO> schedulingConflicts
) {
}
