package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.dto.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.dto.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timetable")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Timetable API", description = "Class scheduling and timetable management")
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
public class TimetableController {

    private final TimetableService timetableService;

    // ================================
    // SCHEDULING OPERATIONS
    // ================================

    @Operation(summary = "Schedule a new class instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class scheduled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data or scheduling conflict")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition or instructor not found")
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<ScheduledInstanceDTO>> scheduleClass(
            @Valid @RequestBody ScheduleRequestDTO request) {
        log.debug("REST request to schedule class for instructor: {} at time: {}", 
            request.instructorUuid(), request.startTime());
        
        ScheduledInstanceDTO result = timetableService.scheduleClass(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Class scheduled successfully"));
    }

    @Operation(summary = "Cancel a scheduled class instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Scheduled instance cancelled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid cancellation request")
    @DeleteMapping("/schedule/{instanceUuid}")
    public ResponseEntity<ApiResponse<Void>> cancelScheduledClass(
            @Parameter(description = "UUID of the scheduled instance to cancel")
            @PathVariable UUID instanceUuid,
            @Parameter(description = "Reason for cancellation")
            @RequestParam String reason) {
        log.debug("REST request to cancel scheduled instance: {} with reason: {}", instanceUuid, reason);
        
        timetableService.cancelScheduledInstance(instanceUuid, reason);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update the status of a scheduled instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status value")
    @PatchMapping("/schedule/{instanceUuid}/status")
    public ResponseEntity<ApiResponse<Void>> updateScheduledInstanceStatus(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid,
            @Parameter(description = "New status (SCHEDULED, ONGOING, COMPLETED, CANCELLED)")
            @RequestParam String status) {
        log.debug("REST request to update status of scheduled instance: {} to: {}", instanceUuid, status);
        
        timetableService.updateScheduledInstanceStatus(instanceUuid, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Status updated successfully"));
    }

    // ================================
    // SCHEDULE QUERIES
    // ================================

    @Operation(summary = "Get a scheduled instance by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scheduled instance retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @GetMapping("/schedule/{instanceUuid}")
    public ResponseEntity<ApiResponse<ScheduledInstanceDTO>> getScheduledInstance(
            @Parameter(description = "UUID of the scheduled instance to retrieve")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to get scheduled instance: {}", instanceUuid);
        
        ScheduledInstanceDTO result = timetableService.getScheduledInstance(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Scheduled instance retrieved successfully"));
    }

    @Operation(summary = "Get schedule for a specific instructor within a date range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Instructor schedule retrieved successfully")
    @GetMapping("/instructor/{instructorUuid}")
    public ResponseEntity<ApiResponse<List<ScheduledInstanceDTO>>> getInstructorSchedule(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.debug("REST request to get schedule for instructor: {} from {} to {}", instructorUuid, start, end);
        
        List<ScheduledInstanceDTO> result = timetableService.getScheduleForInstructor(instructorUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(result, "Instructor schedule retrieved successfully"));
    }

    // ================================
    // CONFLICT CHECKING
    // ================================

    @Operation(summary = "Check if an instructor has scheduling conflicts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conflict check completed")
    @PostMapping("/instructor/{instructorUuid}/check-conflict")
    public ResponseEntity<ApiResponse<Boolean>> checkInstructorConflict(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody ScheduleRequestDTO request) {
        log.debug("REST request to check conflicts for instructor: {}", instructorUuid);
        
        boolean hasConflict = timetableService.hasInstructorConflict(instructorUuid, request);
        return ResponseEntity.ok(ApiResponse.success(hasConflict, 
            hasConflict ? "Instructor has scheduling conflicts" : "No conflicts found"));
    }

    @Operation(summary = "Check if a student has enrollment conflicts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conflict check completed")
    @PostMapping("/student/{studentUuid}/check-conflict")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkStudentConflict(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            @Valid @RequestBody ScheduleRequestDTO request) {
        log.debug("REST request to check conflicts for student: {}", studentUuid);
        
        boolean hasConflict = timetableService.hasStudentConflict(studentUuid, request);
        return ResponseEntity.ok(ApiResponse.success(hasConflict, 
            hasConflict ? "Student has enrollment conflicts" : "No conflicts found"));
    }
}