package apps.sarafrika.elimika.course.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight projection for program progress shared across modules.
 */
public record LearnerProgramProgressView(
        @JsonProperty("enrollment_uuid") UUID enrollmentUuid,
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_name") String programName,
        @JsonProperty("status") String status,
        @JsonProperty("progress_percentage") BigDecimal progressPercentage,
        @JsonProperty("updated_date") LocalDateTime updatedDate
) {
}
