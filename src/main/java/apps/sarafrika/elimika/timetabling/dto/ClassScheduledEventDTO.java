package apps.sarafrika.elimika.timetabling.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event DTO published when a class is scheduled.
 * <p>
 * This event notifies other modules that a class instance has been placed
 * on the calendar and is available for enrollment.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record ClassScheduledEventDTO(
        UUID instanceUuid,
        UUID definitionUuid,
        UUID instructorUuid,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String title,
        String locationType,
        Integer maxParticipants
) {}