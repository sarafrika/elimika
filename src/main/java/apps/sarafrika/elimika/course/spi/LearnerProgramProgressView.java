package apps.sarafrika.elimika.course.spi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight projection for program progress shared across modules.
 */
public record LearnerProgramProgressView(
        UUID enrollmentUuid,
        UUID programUuid,
        String programName,
        String status,
        BigDecimal progressPercentage,
        LocalDateTime updatedDate
) {
}
