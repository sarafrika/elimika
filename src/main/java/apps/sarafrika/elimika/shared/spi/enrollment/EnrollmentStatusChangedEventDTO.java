package apps.sarafrika.elimika.shared.spi.enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a class enrollment status changes.
 */
public record EnrollmentStatusChangedEventDTO(
        UUID enrollmentUuid,
        UUID instanceUuid,
        UUID studentUuid,
        UUID classDefinitionUuid,
        String status,
        LocalDateTime statusChangedAt
) { }
