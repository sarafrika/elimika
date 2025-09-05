package apps.sarafrika.elimika.availability.dto;

import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain event DTO fired when an instructor's availability is changed.
 * <p>
 * This event notifies other modules (like Timetabling) about changes
 * to an instructor's availability that may affect existing schedules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record InstructorAvailabilityChangedEventDTO(
        @NotNull UUID instructorUuid,
        @NotNull AvailabilityType availabilityType,
        @NotNull LocalDate effectiveDate,
        String changeDescription
) {}