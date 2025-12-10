package apps.sarafrika.elimika.timetabling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "BlockInstructorTimeRequest",
        description = "Request to block an instructor's calendar for non-teaching commitments (optional feature). Supports multiple periods."
)
public record BlockInstructorTimeRequest(

        @Schema(description = "Periods to block", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "periods is required")
        @Size(min = 1, message = "At least one block period is required")
        @JsonProperty("periods")
        List<Period> periods
) {

    @Schema(description = "A single blocked period")
    public record Period(
            @Schema(description = "Start time (UTC) for the block", example = "2025-01-20T09:00:00")
            @NotNull(message = "start_time is required")
            @JsonProperty("start_time")
            LocalDateTime startTime,

            @Schema(description = "End time (UTC) for the block", example = "2025-01-20T11:00:00")
            @NotNull(message = "end_time is required")
            @JsonProperty("end_time")
            LocalDateTime endTime,

            @Schema(description = "Optional reason shown on the calendar", example = "Travel / interviews")
            @JsonProperty("reason")
            String reason
    ) {
    }
}
