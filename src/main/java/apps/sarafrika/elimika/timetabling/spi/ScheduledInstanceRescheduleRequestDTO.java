package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(
        name = "ScheduledInstanceRescheduleRequest",
        description = "Request payload for changing the date and time of a scheduled class instance"
)
public record ScheduledInstanceRescheduleRequestDTO(

        @Schema(description = "**[REQUIRED]** New start date and time for the scheduled instance.", format = "date-time")
        @NotNull(message = "Start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "**[REQUIRED]** New end date and time for the scheduled instance.", format = "date-time")
        @NotNull(message = "End time is required")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "**[OPTIONAL]** Timezone for the scheduled instance. Defaults to the existing timezone.")
        @JsonProperty("timezone")
        String timezone
) {
}
