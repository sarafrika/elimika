package apps.sarafrika.elimika.timetabling.dto;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event DTO published when a class enrollment status changes.
 * Used to synchronize downstream modules with the latest enrollment status.
 */
public record EnrollmentStatusChangedEventDTO(
        UUID enrollmentUuid,
        UUID instanceUuid,
        UUID studentUuid,
        UUID classDefinitionUuid,
        EnrollmentStatus status,
        LocalDateTime statusChangedAt
) { }
