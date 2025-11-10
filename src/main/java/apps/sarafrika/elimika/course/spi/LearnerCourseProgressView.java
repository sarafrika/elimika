package apps.sarafrika.elimika.course.spi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight projection shared with other modules for parent dashboards.
 */
public record LearnerCourseProgressView(
        UUID enrollmentUuid,
        UUID courseUuid,
        String courseName,
        String status,
        BigDecimal progressPercentage,
        LocalDateTime updatedDate
) {
}
