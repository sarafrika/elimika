package apps.sarafrika.elimika.shared.spi.enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a student is enrolled in a scheduled class instance.
 */
public record StudentEnrolledEventDTO(
        UUID enrollmentUuid,
        UUID instanceUuid,
        UUID studentUuid,
        UUID classDefinitionUuid,
        UUID instructorUuid,
        LocalDateTime classStartTime,
        String classTitle
) { }
