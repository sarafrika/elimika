package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(
        name = "ClassSessionTemplate",
        description = "Time slot template used during class creation to generate scheduled instances with optional recurrence"
)
public record ClassSessionTemplateDTO(

        @Schema(description = "Start time for the first occurrence (UTC)", example = "2025-01-15T14:00:00")
        @NotNull(message = "Session start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "End time for the first occurrence (UTC)", example = "2025-01-15T15:30:00")
        @NotNull(message = "Session end time is required")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Inline recurrence rule for this session template", nullable = true)
        @JsonProperty("recurrence")
        ClassRecurrenceDTO recurrence,

        @Schema(description = "Conflict handling strategy: FAIL (default), SKIP, ROLLOVER", example = "FAIL")
        @JsonProperty("conflict_resolution")
        ConflictResolutionStrategy conflictResolution
) {
}
