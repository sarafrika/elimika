package apps.sarafrika.elimika.availability.controller;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.dto.InstructorCalendarEntryDTO;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleEntry;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing instructor availability.
 *
 * This controller exposes read-only availability operations and simple management
 * utilities for instructor calendars.
 *
 * Endpoint Structure:
 * - /api/v1/instructors/{instructorUuid}/availability/check - Check availability
 * - /api/v1/instructors/{instructorUuid}/availability/calendar - Merged calendar feed
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@RestController
@RequestMapping("/api/v1/instructors/{instructorUuid}/availability")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Instructor Availability Management",
     description = "APIs for managing instructor availability checks and calendar feeds")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final InstructorScheduleLookupService instructorScheduleLookupService;

    // ================================
    // AVAILABILITY BULK OPERATIONS & CALENDAR
    // ================================

    @Operation(
        summary = "Clear all availability for an instructor",
        description = "Removes all availability slots and patterns for an instructor. Use with caution."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Availability cleared successfully")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearInstructorAvailability(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid) {
        log.debug("REST request to clear all availability for instructor: {}", instructorUuid);

        availabilityService.clearAvailability(instructorUuid);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // AVAILABILITY QUERIES
    // ================================

    @Operation(
        summary = "Get merged instructor calendar",
        description = "Returns a merged feed of availability slots, blocked time, and scheduled instances for the instructor within a date range."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calendar retrieved successfully")
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<InstructorCalendarEntryDTO>>> getInstructorCalendar(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date of the range (YYYY-MM-DD)")
            @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date of the range (YYYY-MM-DD)")
            @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("REST request to get merged calendar for instructor: {} from {} to {}", instructorUuid, startDate, endDate);

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be on or before end date");
        }

        List<InstructorCalendarEntryDTO> entries = new java.util.ArrayList<>();

        startDate.datesUntil(endDate.plusDays(1))
                .forEach(date -> availabilityService.getAvailabilityForDate(instructorUuid, date)
                        .forEach(slot -> entries.add(mapAvailabilityEntry(date, slot))));

        List<InstructorScheduleEntry> scheduledInstances = instructorScheduleLookupService.getScheduleForInstructor(
                instructorUuid, startDate, endDate);
        scheduledInstances.forEach(instance -> entries.add(mapScheduledInstanceEntry(instance)));

        return ResponseEntity.ok(ApiResponse.success(entries, "Instructor calendar retrieved successfully"));
    }

    @Operation(
        summary = "Check if instructor is available during a time period",
        description = """
            Checks whether an instructor is available for the entire specified time period.

            Returns true unless a blocked slot overlaps the requested window.
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability check completed")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkAvailability(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date and time (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date and time (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.debug("REST request to check availability for instructor: {} from {} to {}",
                instructorUuid, start, end);

        boolean isAvailable = availabilityService.isInstructorAvailable(instructorUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(isAvailable,
                isAvailable ? "Instructor is available" : "Instructor is not available"));
    }

    private InstructorCalendarEntryDTO mapAvailabilityEntry(LocalDate date, AvailabilitySlotDTO slot) {
        LocalTime startTime = slot.startTime();
        LocalTime endTime = slot.endTime();

        return new InstructorCalendarEntryDTO(
                slot.uuid(),
                Boolean.TRUE.equals(slot.isAvailable())
                        ? InstructorCalendarEntryDTO.CalendarEntryType.AVAILABILITY
                        : InstructorCalendarEntryDTO.CalendarEntryType.BLOCKED,
                date.atTime(startTime),
                date.atTime(endTime),
                slot.availabilityType(),
                slot.isAvailable(),
                null,
                null,
                null,
                null,
                slot.customPattern()
        );
    }

    private InstructorCalendarEntryDTO mapScheduledInstanceEntry(InstructorScheduleEntry instance) {
        return new InstructorCalendarEntryDTO(
                instance.uuid(),
                InstructorCalendarEntryDTO.CalendarEntryType.SCHEDULED_INSTANCE,
                instance.startTime(),
                instance.endTime(),
                null,
                false,
                instance.status(),
                instance.title(),
                instance.classDefinitionUuid(),
                instance.locationType(),
                instance.cancellationReason()
        );
    }
}
