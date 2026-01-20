package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "ClassSchedulingConflict",
        description = "Details of a conflicting schedule request during class creation"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassSchedulingConflictDTO(

        @Schema(description = "Requested start date-time that conflicted", example = "2025-01-15T14:00:00")
        @JsonProperty("requested_start")
        LocalDateTime requestedStart,

        @Schema(description = "Requested end date-time that conflicted", example = "2025-01-15T15:30:00")
        @JsonProperty("requested_end")
        LocalDateTime requestedEnd,

        @Schema(description = "Reasons for the conflict")
        @JsonProperty("reasons")
        List<String> reasons
) {
}
