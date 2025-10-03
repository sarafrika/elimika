package apps.sarafrika.elimika.availability.controller;

import apps.sarafrika.elimika.availability.dto.*;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
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
 * - /api/v1/instructors/{instructorUuid}/availability - Manage all availability
 * - /api/v1/instructors/{instructorUuid}/availability/slots - Manage individual slots
 * - /api/v1/instructors/{instructorUuid}/availability/patterns - Set recurring patterns
 * - /api/v1/instructors/{instructorUuid}/availability/check - Check availability
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

    // ================================
    // AVAILABILITY OVERVIEW & BULK OPERATIONS
    // ================================

    @Operation(
        summary = "Get all availability for an instructor",
        description = "Retrieves all availability slots for a specific instructor, including all patterns and blocked times"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getInstructorAvailability(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid) {
        log.debug("REST request to get all availability for instructor: {}", instructorUuid);

        List<AvailabilitySlotDTO> result = availabilityService.getAvailabilityForInstructor(instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Instructor availability retrieved successfully"));
    }

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
    // INDIVIDUAL SLOT MANAGEMENT
    // ================================

    @Operation(
        summary = "Create a new availability slot",
        description = "Creates a single availability slot for an instructor"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Availability slot created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/slots")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> createAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Valid @RequestBody AvailabilitySlotDTO request) {
        log.debug("REST request to create availability slot for instructor: {}", instructorUuid);

        AvailabilitySlotDTO result = availabilityService.createAvailabilitySlot(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Availability slot created successfully"));
    }

    @Operation(
        summary = "Get a specific availability slot",
        description = "Retrieves a single availability slot by its UUID"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability slot retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Availability slot not found")
    @GetMapping("/slots/{slotUuid}")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> getAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "UUID of the availability slot") @PathVariable UUID slotUuid) {
        log.debug("REST request to get availability slot: {} for instructor: {}", slotUuid, instructorUuid);

        AvailabilitySlotDTO result = availabilityService.getAvailabilitySlot(slotUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Availability slot retrieved successfully"));
    }

    @Operation(
        summary = "Update an availability slot",
        description = "Updates an existing availability slot"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability slot updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Availability slot not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/slots/{slotUuid}")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> updateAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "UUID of the availability slot") @PathVariable UUID slotUuid,
            @Valid @RequestBody AvailabilitySlotDTO request) {
        log.debug("REST request to update availability slot: {} for instructor: {}", slotUuid, instructorUuid);

        AvailabilitySlotDTO result = availabilityService.updateAvailabilitySlot(slotUuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Availability slot updated successfully"));
    }

    @Operation(
        summary = "Delete an availability slot",
        description = "Removes a specific availability slot"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Availability slot deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Availability slot not found")
    @DeleteMapping("/slots/{slotUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteAvailabilitySlot(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "UUID of the availability slot") @PathVariable UUID slotUuid) {
        log.debug("REST request to delete availability slot: {} for instructor: {}", slotUuid, instructorUuid);

        availabilityService.deleteAvailabilitySlot(slotUuid);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // RECURRING PATTERN MANAGEMENT
    // ================================

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
        summary = "Get availability for a specific date",
        description = "Retrieves all availability slots (including from patterns) for an instructor on a specific date"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability for date retrieved successfully")
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailabilityForDate(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "Date to check (YYYY-MM-DD)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("REST request to get availability for instructor: {} on date: {}", instructorUuid, date);

        List<AvailabilitySlotDTO> result = availabilityService.getAvailabilityForDate(instructorUuid, date);
        return ResponseEntity.ok(ApiResponse.success(result, "Availability for date retrieved successfully"));
    }

    @Operation(
        summary = "Find available slots within a date range",
        description = """
            Finds all available time slots for an instructor within a specified date range.

            This is useful for scheduling systems that need to:
            - Show available booking slots
            - Find the next available time
            - Display a calendar of availability

            Only returns slots where isAvailable = true (excludes blocked times).
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Available slots found successfully")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> findAvailableSlots(
            @Parameter(description = "UUID of the instructor") @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date of search range (YYYY-MM-DD)")
            @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date of search range (YYYY-MM-DD)")
            @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("REST request to find available slots for instructor: {} from {} to {}",
                instructorUuid, startDate, endDate);

        List<AvailabilitySlotDTO> result = availabilityService.findAvailableSlots(instructorUuid, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result, "Available slots found successfully"));
    }

    @Operation(
        summary = "Check if instructor is available during a time period",
        description = """
            Checks whether an instructor is available for the entire specified time period.

            Returns true only if the instructor is available for the ENTIRE duration.
            This considers:
            - All availability patterns
            - Blocked time slots
            - Existing bookings (if integrated with scheduling)

            Useful for validating booking requests before creating them.
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
}