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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Instructor Availability API", description = "Complete instructor availability management including daily, weekly, monthly, and custom patterns")
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    // ================================
    // AVAILABILITY SLOT MANAGEMENT
    // ================================

    @Operation(summary = "Create a new availability slot")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Availability slot created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> createAvailabilitySlot(
            @Valid @RequestBody AvailabilitySlotDTO request) {
        log.debug("REST request to create availability slot for instructor: {}", request.instructorUuid());
        
        AvailabilitySlotDTO result = availabilityService.createAvailabilitySlot(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Availability slot created successfully"));
    }

    @Operation(summary = "Get an availability slot by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability slot retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Availability slot not found")
    @GetMapping("/slots/{uuid}")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> getAvailabilitySlot(
            @Parameter(description = "UUID of the availability slot to retrieve")
            @PathVariable UUID uuid) {
        log.debug("REST request to get availability slot: {}", uuid);
        
        AvailabilitySlotDTO result = availabilityService.getAvailabilitySlot(uuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Availability slot retrieved successfully"));
    }

    @Operation(summary = "Update an existing availability slot")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability slot updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Availability slot not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/slots/{uuid}")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> updateAvailabilitySlot(
            @Parameter(description = "UUID of the availability slot to update")
            @PathVariable UUID uuid,
            @Valid @RequestBody AvailabilitySlotDTO request) {
        log.debug("REST request to update availability slot: {}", uuid);
        
        AvailabilitySlotDTO result = availabilityService.updateAvailabilitySlot(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Availability slot updated successfully"));
    }

    @Operation(summary = "Delete an availability slot")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Availability slot deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Availability slot not found")
    @DeleteMapping("/slots/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteAvailabilitySlot(
            @Parameter(description = "UUID of the availability slot to delete")
            @PathVariable UUID uuid) {
        log.debug("REST request to delete availability slot: {}", uuid);
        
        availabilityService.deleteAvailabilitySlot(uuid);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // INSTRUCTOR AVAILABILITY MANAGEMENT
    // ================================

    @Operation(summary = "Get all availability for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Instructor availability retrieved successfully")
    @GetMapping("/instructors/{instructorUuid}")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailabilityForInstructor(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid) {
        log.debug("REST request to get availability for instructor: {}", instructorUuid);
        
        List<AvailabilitySlotDTO> result = availabilityService.getAvailabilityForInstructor(instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Instructor availability retrieved successfully"));
    }

    @Operation(summary = "Get availability for an instructor on a specific date")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability for date retrieved successfully")
    @GetMapping("/instructors/{instructorUuid}/date/{date}")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailabilityForDate(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Date to check availability for (YYYY-MM-DD)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("REST request to get availability for instructor: {} on date: {}", instructorUuid, date);
        
        List<AvailabilitySlotDTO> result = availabilityService.getAvailabilityForDate(instructorUuid, date);
        return ResponseEntity.ok(ApiResponse.success(result, "Availability for date retrieved successfully"));
    }

    @Operation(summary = "Get available slots for an instructor on a specific date")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Available slots retrieved successfully")
    @GetMapping("/instructors/{instructorUuid}/available/{date}")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailableSlots(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Date to check for available slots (YYYY-MM-DD)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("REST request to get available slots for instructor: {} on date: {}", instructorUuid, date);
        
        List<AvailabilitySlotDTO> result = availabilityService.getAvailableSlots(instructorUuid, date);
        return ResponseEntity.ok(ApiResponse.success(result, "Available slots retrieved successfully"));
    }

    @Operation(summary = "Get blocked slots for an instructor on a specific date")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Blocked slots retrieved successfully")
    @GetMapping("/instructors/{instructorUuid}/blocked/{date}")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getBlockedSlots(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Date to check for blocked slots (YYYY-MM-DD)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("REST request to get blocked slots for instructor: {} on date: {}", instructorUuid, date);
        
        List<AvailabilitySlotDTO> result = availabilityService.getBlockedSlots(instructorUuid, date);
        return ResponseEntity.ok(ApiResponse.success(result, "Blocked slots retrieved successfully"));
    }

    // ================================
    // PATTERN-BASED AVAILABILITY MANAGEMENT
    // ================================

    @Operation(summary = "Set weekly availability patterns for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly availability set successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/instructors/{instructorUuid}/weekly")
    public ResponseEntity<ApiResponse<Void>> setWeeklyAvailability(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody List<WeeklyAvailabilitySlotDTO> slots) {
        log.debug("REST request to set weekly availability for instructor: {}", instructorUuid);
        
        availabilityService.setWeeklyAvailability(instructorUuid, slots);
        return ResponseEntity.ok(ApiResponse.success(null, "Weekly availability set successfully"));
    }

    @Operation(summary = "Set daily availability patterns for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Daily availability set successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/instructors/{instructorUuid}/daily")
    public ResponseEntity<ApiResponse<Void>> setDailyAvailability(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody List<DailyAvailabilitySlotDTO> slots) {
        log.debug("REST request to set daily availability for instructor: {}", instructorUuid);
        
        availabilityService.setDailyAvailability(instructorUuid, slots);
        return ResponseEntity.ok(ApiResponse.success(null, "Daily availability set successfully"));
    }

    @Operation(summary = "Set monthly availability patterns for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly availability set successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/instructors/{instructorUuid}/monthly")
    public ResponseEntity<ApiResponse<Void>> setMonthlyAvailability(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody List<MonthlyAvailabilitySlotDTO> slots) {
        log.debug("REST request to set monthly availability for instructor: {}", instructorUuid);
        
        availabilityService.setMonthlyAvailability(instructorUuid, slots);
        return ResponseEntity.ok(ApiResponse.success(null, "Monthly availability set successfully"));
    }

    @Operation(summary = "Set custom availability patterns for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Custom availability set successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/instructors/{instructorUuid}/custom")
    public ResponseEntity<ApiResponse<Void>> setCustomAvailability(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody List<CustomAvailabilitySlotDTO> slots) {
        log.debug("REST request to set custom availability for instructor: {}", instructorUuid);
        
        availabilityService.setCustomAvailability(instructorUuid, slots);
        return ResponseEntity.ok(ApiResponse.success(null, "Custom availability set successfully"));
    }

    // ================================
    // AVAILABILITY CHECKING AND UTILITY OPERATIONS
    // ================================

    @Operation(summary = "Check if an instructor is available during a time period")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability check completed")
    @GetMapping("/instructors/{instructorUuid}/check")
    public ResponseEntity<ApiResponse<Boolean>> checkAvailability(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date and time (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date and time (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.debug("REST request to check availability for instructor: {} from {} to {}", instructorUuid, start, end);
        
        boolean isAvailable = availabilityService.isInstructorAvailable(instructorUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(isAvailable, 
            isAvailable ? "Instructor is available" : "Instructor is not available"));
    }

    @Operation(summary = "Find available slots for an instructor within a date range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Available slots found successfully")
    @GetMapping("/instructors/{instructorUuid}/find-available")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> findAvailableSlots(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date of the search range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date of the search range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("REST request to find available slots for instructor: {} from {} to {}", instructorUuid, startDate, endDate);
        
        List<AvailabilitySlotDTO> result = availabilityService.findAvailableSlots(instructorUuid, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result, "Available slots found successfully"));
    }

    @Operation(summary = "Clear all availability for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Availability cleared successfully")
    @DeleteMapping("/instructors/{instructorUuid}")
    public ResponseEntity<ApiResponse<Void>> clearAvailability(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid) {
        log.debug("REST request to clear availability for instructor: {}", instructorUuid);
        
        availabilityService.clearAvailability(instructorUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Block time for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Time blocked successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/instructors/{instructorUuid}/block")
    public ResponseEntity<ApiResponse<Void>> blockTime(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date and time to block (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date and time to block (ISO format: YYYY-MM-DDTHH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.debug("REST request to block time for instructor: {} from {} to {}", instructorUuid, start, end);
        
        availabilityService.blockTime(instructorUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(null, "Time blocked successfully"));
    }
}