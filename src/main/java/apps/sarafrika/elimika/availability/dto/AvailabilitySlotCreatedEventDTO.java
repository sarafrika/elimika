package apps.sarafrika.elimika.availability.dto;

import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Domain event DTO fired when a new availability slot is created.
 * <p>
 * This event notifies other modules (like Timetabling) about new
 * availability slots that may open up scheduling opportunities.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record AvailabilitySlotCreatedEventDTO(
        @NotNull UUID slotUuid,
        @NotNull UUID instructorUuid,
        @NotNull AvailabilityType availabilityType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Boolean isAvailable,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate
) {}