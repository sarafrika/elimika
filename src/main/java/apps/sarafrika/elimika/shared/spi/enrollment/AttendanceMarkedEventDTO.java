package apps.sarafrika.elimika.shared.spi.enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when attendance is marked for a class enrollment.
 */
public record AttendanceMarkedEventDTO(
        UUID enrollmentUuid,
        UUID instanceUuid,
        UUID studentUuid,
        UUID classDefinitionUuid,
        UUID instructorUuid,
        String attendanceStatus,
        LocalDateTime markedAt,
        String classTitle
) { }
