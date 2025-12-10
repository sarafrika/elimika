package apps.sarafrika.elimika.availability.controller;

import apps.sarafrika.elimika.availability.dto.*;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleEntry;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * This controller provides a clean, RESTful API for managing availability patterns
 * including individual slots, recurring patterns (daily, weekly, monthly, custom),
 * and availability checking operations.
 *
 * Endpoint Structure:
 * - /api/v1/instructors/{instructorUuid}/availability/patterns - Set recurring patterns
 * - /api/v1/instructors/{instructorUuid}/availability/check - Check availability
 * - /api/v1/instructors/{instructorUuid}/availability/block - Block time
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
     description = "APIs for managing instructor availability, including recurring patterns, blocked time, and availability checks")
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

    @Operation(
        summary = "Set availability patterns",
        description = """
            Sets recurring availability patterns for an instructor.

            Supports multiple pattern types:
            - **weekly**: Patterns based on day of week (Monday-Sunday)
            - **daily**: Patterns that repeat daily
            - **monthly**: Patterns based on day of month (1-31)
            - **custom**: Custom recurring patterns with specific rules

            The pattern type is determined by the request body structure.
            Use the appropriate DTO for the pattern type you want to set.

            Examples:
            - Weekly: Set availability every Monday and Wednesday 9am-5pm
            - Daily: Set availability every day 2pm-4pm
            - Monthly: Set availability on the 1st and 15th of every month
            - Custom: Set availability with custom recurrence rules
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability patterns set successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pattern data")
    @PostMapping("/patterns")
    public ResponseEntity<ApiResponse<Void>> setAvailabilityPatterns(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "Pattern type: weekly, daily, monthly, or custom")
            @RequestParam("pattern_type") String patternType,
            @Valid @RequestBody Object patterns) {
        log.debug("REST request to set {} availability patterns for instructor: {}", patternType, instructorUuid);

        switch (patternType.toLowerCase()) {
            case "weekly" -> {
                @SuppressWarnings("unchecked")
                List<WeeklyAvailabilitySlotDTO> weeklySlots = (List<WeeklyAvailabilitySlotDTO>) patterns;
                availabilityService.setWeeklyAvailability(instructorUuid, weeklySlots);
            }
            case "daily" -> {
                @SuppressWarnings("unchecked")
                List<DailyAvailabilitySlotDTO> dailySlots = (List<DailyAvailabilitySlotDTO>) patterns;
                availabilityService.setDailyAvailability(instructorUuid, dailySlots);
            }
            case "monthly" -> {
                @SuppressWarnings("unchecked")
                List<MonthlyAvailabilitySlotDTO> monthlySlots = (List<MonthlyAvailabilitySlotDTO>) patterns;
                availabilityService.setMonthlyAvailability(instructorUuid, monthlySlots);
            }
            case "custom" -> {
                @SuppressWarnings("unchecked")
                List<CustomAvailabilitySlotDTO> customSlots = (List<CustomAvailabilitySlotDTO>) patterns;
                availabilityService.setCustomAvailability(instructorUuid, customSlots);
            }
            default -> throw new IllegalArgumentException("Invalid pattern type: " + patternType +
                    ". Supported types: weekly, daily, monthly, custom");
        }

        return ResponseEntity.ok(ApiResponse.success(null,
                patternType.substring(0, 1).toUpperCase() + patternType.substring(1) + " availability patterns set successfully"));
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

    // ================================
    // BLOCKING TIME
    // ================================

    @Operation(
        summary = "Block time for an instructor",
        description = """
            Blocks a specific time period for an instructor, making them unavailable.

            This creates availability slots with isAvailable = false, which override
            any existing availability patterns for that time period.

            You can optionally provide a color code (hex format) to categorize and
            visually distinguish different types of blocked times on the frontend.

            Common use cases:
            - Marking vacation time (e.g., color_code: "#FF6B6B" - red)
            - Blocking time for meetings (e.g., color_code: "#FFD93D" - yellow)
            - Indicating sick leave (e.g., color_code: "#FFA07A" - orange)
            - Personal time off (e.g., color_code: "#95E1D3" - teal)
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time blocked successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid time range or color code format")
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<Void>> blockTime(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date and time to block (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date and time to block (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Optional hex color code for UI visualization (e.g., #FF6B6B)")
            @RequestParam(value = "color_code", required = false) String colorCode) {
        log.debug("REST request to block time for instructor: {} from {} to {} with color: {}",
                instructorUuid, start, end, colorCode);

        availabilityService.blockTime(instructorUuid, start, end, colorCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Time blocked successfully"));
    }

    @Operation(
        summary = "Block multiple time slots for an instructor",
        description = """
            Blocks multiple specific time periods for an instructor in one request, making them unavailable.

            Each slot creates an availability entry with isAvailable = false and can optionally specify
            a color code for frontend visualization (e.g., distinguishing PTO vs. meetings).
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time slots blocked successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid time range or color code format")
    @PostMapping("/block/bulk")
    public ResponseEntity<ApiResponse<Void>> blockTimeSlots(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Valid @RequestBody BlockTimeSlotsRequestDTO request) {
        int slotCount = request != null && request.slots() != null ? request.slots().size() : 0;
        log.debug("REST request to block {} time slots for instructor: {}", slotCount, instructorUuid);

        if (request == null) {
            throw new IllegalArgumentException("Request payload cannot be null");
        }

        availabilityService.blockTimeSlots(instructorUuid, request.slots());
        return ResponseEntity.ok(ApiResponse.success(null, "Time slots blocked successfully"));
    }

    // ================================
    // STUDENT BOOKING
    // ================================

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
