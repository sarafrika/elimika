package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "StudentCourseOverviewSession",
        description = "The learner's current or next sitting of a class."
)
public record StudentCourseOverviewSessionDTO(

        @Schema(description = "Scheduled instance identifier")
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid,

        @Schema(description = "Session title")
        @JsonProperty("title")
        String title,

        @Schema(description = "Session start (UTC)", format = "date-time")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "Session end (UTC)", format = "date-time")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Timezone the session was scheduled in")
        @JsonProperty("timezone")
        String timezone,

        @Schema(description = "Location type, e.g. ONLINE or IN_PERSON")
        @JsonProperty("location_type")
        String locationType,

        @Schema(description = "Location name")
        @JsonProperty("location_name")
        String locationName,

        @Schema(description = "Instructor teaching this sitting")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid
) {
}
