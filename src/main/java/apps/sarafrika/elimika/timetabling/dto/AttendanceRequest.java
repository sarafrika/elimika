package apps.sarafrika.elimika.timetabling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "AttendanceRequest", description = "One learner's attendance for a session, with the instructor's rich-text comment")
public record AttendanceRequest(
        @Schema(description = "Whether the student attended (true) or was absent (false)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "attended is required")
        @JsonProperty("attended")
        Boolean attended,

        @Schema(description = "Condition of the learner's instrument: GOOD, NEEDS_REPAIR or NO_EQUIPMENT")
        @JsonProperty("instrument_condition")
        String instrumentCondition,

        @Schema(description = "The instructor's comment on this learner for the session (HTML)")
        @Size(max = 20000, message = "comment must be at most 20000 characters")
        @JsonProperty("comment")
        String comment
) {
}
