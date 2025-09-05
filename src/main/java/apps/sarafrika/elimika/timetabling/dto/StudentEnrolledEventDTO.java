package apps.sarafrika.elimika.timetabling.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event DTO published when a student is enrolled in a scheduled instance.
 * <p>
 * This event notifies other modules about new student enrollments,
 * which can trigger notifications, capacity updates, or other business logic.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record StudentEnrolledEventDTO(
        UUID enrollmentUuid,
        UUID instanceUuid,
        UUID studentUuid,
        UUID classDefinitionUuid,
        UUID instructorUuid,
        LocalDateTime classStartTime,
        String classTitle
) {}