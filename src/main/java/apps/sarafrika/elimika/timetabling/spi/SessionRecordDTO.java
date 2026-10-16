package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "SessionRecord", description = "What was taught in one session: topic covered, instructor remarks and duration")
public record SessionRecordDTO(
        @JsonProperty(value = "instance_uuid", access = JsonProperty.Access.READ_ONLY) UUID instanceUuid,
        @JsonProperty(value = "lesson_uuid", access = JsonProperty.Access.READ_ONLY) UUID lessonUuid,
        @JsonProperty(value = "title", access = JsonProperty.Access.READ_ONLY) String title,
        @JsonProperty("topic_covered") @Size(max = 5000) String topicCovered,
        @JsonProperty("instructor_remarks") @Size(max = 5000) String instructorRemarks,
        @JsonProperty(value = "start_time", access = JsonProperty.Access.READ_ONLY) LocalDateTime startTime,
        @JsonProperty(value = "end_time", access = JsonProperty.Access.READ_ONLY) LocalDateTime endTime,
        @JsonProperty(value = "duration_minutes", access = JsonProperty.Access.READ_ONLY) Long durationMinutes
) {
}
