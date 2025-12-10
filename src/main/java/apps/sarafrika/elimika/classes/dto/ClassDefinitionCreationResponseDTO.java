package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "ClassDefinitionCreationResponse",
        description = "Response payload for class definition creation including scheduled instances and conflicts"
)
public record ClassDefinitionCreationResponseDTO(

        @Schema(description = "Persisted class definition")
        @JsonProperty("class_definition")
        ClassDefinitionDTO classDefinition,

        @Schema(description = "Instances scheduled from embedded session templates")
        @JsonProperty("scheduled_instances")
        List<ScheduledInstanceDTO> scheduledInstances,

        @Schema(description = "Conflicts encountered while scheduling")
        @JsonProperty("scheduling_conflicts")
        List<ClassSchedulingConflictDTO> schedulingConflicts
) {
}
