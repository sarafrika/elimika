package apps.sarafrika.elimika.availability.controller;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.dto.InstructorCalendarEntryDTO;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleEntry;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * <p>Access falls into three bands. Every write — clearing a calendar, and creating, updating or
 * deleting a slot — belongs to the instructor named in the path, or to a platform admin; the two
 * slot-specific writes additionally require the slot to be on that instructor's calendar, which the
 * availability service enforces as it loads it. The raw slot listing is the instructor's own
 * configuration record and is restricted the same way. Free/busy is the exception: a student
 * choosing when to book has to see it, so {@code /calendar} and {@code /check} answer any signed-in
 * caller — but {@code /calendar} hands anyone other than the instructor a redacted feed, windows
 * without the sessions that fill them.</p>
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
    private final DomainSecurityService domainSecurityService;

    // ================================
    // AVAILABILITY BULK OPERATIONS & CALENDAR
    // ================================

    @Operation(
        summary = "Clear all availability for an instructor",
        description = "Removes all availability slots and patterns for an instructor. Use with caution."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Availability cleared successfully")
    @DeleteMapping
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()")
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
        description = """
            Returns a merged feed of availability slots, blocked time, and scheduled instances for
            the instructor within a date range.

            Anyone signed in may read it, because choosing when to book an instructor means seeing
            which windows are free. Callers other than the instructor themselves get each entry
            reduced to its window and whether it is free: titles, the class and organisation behind
            a session, its location and any cancellation reason are dropped.
            """
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

        List<InstructorCalendarEntryDTO> visible = ownsCalendar(instructorUuid)
                ? entries
                : entries.stream().map(InstructorCalendarEntryDTO::redacted).toList();

        return ResponseEntity.ok(ApiResponse.success(visible, "Instructor calendar retrieved successfully"));
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

    // ================================
    // AVAILABILITY SLOT CRUD
    // ================================

    @Operation(
        summary = "List availability slots for an instructor",
        description = "Returns all availability slots configured for the instructor. Restricted to the instructor themselves."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability slots retrieved successfully")
    @GetMapping("/slots")
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailabilitySlots(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid) {
        log.debug("REST request to list availability slots for instructor: {}", instructorUuid);
        List<AvailabilitySlotDTO> slots = availabilityService.getAvailabilityForInstructor(instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(slots, "Availability slots retrieved successfully"));
    }

    @Operation(
        summary = "Create an availability slot for an instructor",
        description = "Creates a new availability slot for the instructor."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Availability slot created successfully")
    @PostMapping("/slots")
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> createAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Valid @RequestBody AvailabilitySlotDTO slot) {
        log.debug("REST request to create availability slot for instructor: {}", instructorUuid);
        validateInstructorMatch(instructorUuid, slot.instructorUuid());
        AvailabilitySlotDTO created = availabilityService.createAvailabilitySlot(slot);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Availability slot created successfully"));
    }

    @Operation(
        summary = "Update an availability slot",
        description = "Updates an existing availability slot for the instructor."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability slot updated successfully")
    @PutMapping("/slots/{slotUuid}")
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> updateAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "UUID of the availability slot") @PathVariable UUID slotUuid,
            @Valid @RequestBody AvailabilitySlotDTO slot) {
        log.debug("REST request to update availability slot {} for instructor: {}", slotUuid, instructorUuid);
        validateInstructorMatch(instructorUuid, slot.instructorUuid());
        AvailabilitySlotDTO updated = availabilityService.updateAvailabilitySlot(instructorUuid, slotUuid, slot);
        return ResponseEntity.ok(ApiResponse.success(updated, "Availability slot updated successfully"));
    }

    @Operation(
        summary = "Delete a single availability slot",
        description = "Removes one availability slot belonging to the instructor."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Availability slot deleted successfully")
    @DeleteMapping("/slots/{slotUuid}")
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<Void>> deleteAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "UUID of the availability slot") @PathVariable UUID slotUuid) {
        log.debug("REST request to delete availability slot {} for instructor: {}", slotUuid, instructorUuid);
        availabilityService.deleteAvailabilitySlot(instructorUuid, slotUuid);
        return ResponseEntity.noContent().build();
    }

    private void validateInstructorMatch(UUID pathInstructorUuid, UUID bodyInstructorUuid) {
        if (bodyInstructorUuid != null && !bodyInstructorUuid.equals(pathInstructorUuid)) {
            throw new IllegalArgumentException(
                    "instructor_uuid in the request body must match the instructor in the path");
        }
    }

    private boolean ownsCalendar(UUID instructorUuid) {
        return domainSecurityService.isInstructorWithUuid(instructorUuid)
                || domainSecurityService.isPlatformAdmin();
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
                slot.customPattern(),
                null,
                null
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
                instance.cancellationReason(),
                instance.organisationUuid(),
                instance.organisationName()
        );
    }
}
