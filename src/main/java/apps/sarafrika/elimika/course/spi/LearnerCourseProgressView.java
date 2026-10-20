package apps.sarafrika.elimika.course.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight projection shared with other modules for parent dashboards.
 */
public record LearnerCourseProgressView(
        @JsonProperty("enrollment_uuid") UUID enrollmentUuid,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("course_name") String courseName,
        @JsonProperty("status") String status,
        @JsonProperty("progress_percentage") BigDecimal progressPercentage,
        @JsonProperty("updated_date") LocalDateTime updatedDate
) {
}
