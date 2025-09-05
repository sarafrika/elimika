package apps.sarafrika.elimika.availability.dto;

import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Domain event DTO fired when an availability slot is deleted.
 * <p>
 * This event notifies other modules (like Timetabling) about deleted
 * availability slots that may affect existing schedules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record AvailabilitySlotDeletedEventDTO(
        @NotNull UUID slotUuid,
        @NotNull UUID instructorUuid,
        @NotNull AvailabilityType availabilityType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Boolean wasAvailable,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate
) {}