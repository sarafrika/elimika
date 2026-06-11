package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "ClassSessionTemplateScheduleResponse",
        description = "Result of adding a session template to an existing class schedule"
)
public record ClassSessionTemplateScheduleResponseDTO(

        @Schema(description = "**[READ-ONLY]** Class definition that received the new template.")
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "**[READ-ONLY]** Persisted session template.")
        @JsonProperty("session_template")
        ClassSessionTemplateDTO sessionTemplate,

        @Schema(description = "**[READ-ONLY]** Scheduled instances created from the template.")
        @JsonProperty("scheduled_instances")
        List<ScheduledInstanceDTO> scheduledInstances,

        @Schema(description = "**[READ-ONLY]** Non-blocking conflicts recorded while applying the template.")
        @JsonProperty("scheduling_conflicts")
        List<ClassSchedulingConflictDTO> schedulingConflicts
) {
}
