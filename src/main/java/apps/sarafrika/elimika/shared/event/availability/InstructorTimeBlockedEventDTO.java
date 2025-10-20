package apps.sarafrika.elimika.shared.event.availability;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event DTO fired when an instructor blocks time in their schedule.
 * <p>
 * This event notifies other modules (like Timetabling) about blocked time
 * that may affect existing classes or prevent new scheduling.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record InstructorTimeBlockedEventDTO(
        @NotNull UUID instructorUuid,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String reason
) {}