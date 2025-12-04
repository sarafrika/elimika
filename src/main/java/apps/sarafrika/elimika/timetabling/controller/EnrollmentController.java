package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;
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
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment API", description = "Student enrollment and attendance management")
public class EnrollmentController {

    private final TimetableService timetableService;

    // ================================
    // ENROLLMENT OPERATIONS
    // ================================

    @Operation(summary = "Enroll a student into a class across all scheduled instances")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Student enrolled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid enrollment request or conflicts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition or scheduled instances not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "Payment required before enrollment is permitted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Student already enrolled")
    @PostMapping
    @PreAuthorize("@domainSecurityService.isStudentOrInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> enrollStudent(
            @Valid @RequestBody EnrollmentRequestDTO request) {
        log.debug("REST request to enroll student: {} into class definition: {}",
            request.studentUuid(), request.classDefinitionUuid());

        List<EnrollmentDTO> result = timetableService.enrollStudent(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Student enrolled into all scheduled class instances"));
    }

    @Operation(summary = "Join class waitlist when capacity is full")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Student added to waitlist")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Waitlist disabled or class has available seats")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class or scheduled instances not found")
    @PostMapping("/waitlist")
    @PreAuthorize("@domainSecurityService.isStudentOrInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> joinWaitlist(
            @Valid @RequestBody EnrollmentRequestDTO request) {
        log.debug("REST request to join waitlist for student: {} and class definition: {}",
                request.studentUuid(), request.classDefinitionUuid());

        List<EnrollmentDTO> result = timetableService.joinWaitlist(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Student added to class waitlist"));
    }

    @Operation(summary = "Cancel a student enrollment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Enrollment cancelled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid cancellation request")
    @DeleteMapping("/{enrollmentUuid}")
    @PreAuthorize("@enrollmentSecurityService.isOwner(#enrollmentUuid) or @domainSecurityService.isInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<Void>> cancelEnrollment(
            @Parameter(description = "UUID of the enrollment to cancel")
            @PathVariable UUID enrollmentUuid,
            @Parameter(description = "Reason for cancellation")
            @RequestParam String reason) {
        log.debug("REST request to cancel enrollment: {} with reason: {}", enrollmentUuid, reason);

        timetableService.cancelEnrollment(enrollmentUuid, reason);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // ATTENDANCE MANAGEMENT
    // ================================

    @Operation(summary = "Mark attendance for a student enrollment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance marked successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Attendance already marked")
    @PatchMapping("/{enrollmentUuid}/attendance")
    @PreAuthorize("@domainSecurityService.isInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<Void>> markAttendance(
            @Parameter(description = "UUID of the enrollment")
            @PathVariable UUID enrollmentUuid,
            @Parameter(description = "Whether the student attended (true) or was absent (false)")
            @RequestParam boolean attended) {
        log.debug("REST request to mark attendance for enrollment: {} as: {}",
            enrollmentUuid, attended ? "ATTENDED" : "ABSENT");

        timetableService.markAttendance(enrollmentUuid, attended);
        return ResponseEntity.ok(ApiResponse.success(null, "Attendance marked successfully"));
    }

    // ================================
    // ENROLLMENT QUERIES
    // ================================

    @Operation(summary = "Get an enrollment by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    @GetMapping("/{enrollmentUuid}")
    @PreAuthorize("@enrollmentSecurityService.isOwner(#enrollmentUuid) or @domainSecurityService.isInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> getEnrollment(
            @Parameter(description = "UUID of the enrollment to retrieve")
            @PathVariable UUID enrollmentUuid) {
        log.debug("REST request to get enrollment: {}", enrollmentUuid);

        EnrollmentDTO result = timetableService.getEnrollment(enrollmentUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Enrollment retrieved successfully"));
    }

    @Operation(summary = "Get all enrollments for a scheduled instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    @GetMapping("/instance/{instanceUuid}")
    @PreAuthorize("@domainSecurityService.isInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getEnrollmentsForInstance(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to get enrollments for scheduled instance: {}", instanceUuid);

        List<EnrollmentDTO> result = timetableService.getEnrollmentsForInstance(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Enrollments retrieved successfully"));
    }

    @Operation(summary = "Get schedule for a specific student within a date range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student schedule retrieved successfully")
    @GetMapping("/student/{studentUuid}")
    @PreAuthorize("@enrollmentSecurityService.isOwner(#studentUuid, 'student') or @domainSecurityService.isInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<List<StudentScheduleDTO>>> getStudentSchedule(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            @Parameter(description = "Start date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.debug("REST request to get schedule for student: {} from {} to {}", studentUuid, start, end);

        List<StudentScheduleDTO> result = timetableService.getScheduleForStudent(studentUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(result, "Student schedule retrieved successfully"));
    }

    // ================================
    // CAPACITY AND STATISTICS
    // ================================

    @Operation(summary = "Get enrollment count for a scheduled instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment count retrieved successfully")
    @GetMapping("/instance/{instanceUuid}/count")
    @PreAuthorize("@domainSecurityService.isInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<Long>> getEnrollmentCount(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to get enrollment count for instance: {}", instanceUuid);

        long count = timetableService.getEnrollmentCount(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(count, "Enrollment count retrieved successfully"));
    }

    @Operation(summary = "Check if a scheduled instance has capacity for new enrollments")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Capacity check completed")
    @GetMapping("/instance/{instanceUuid}/capacity")
    @PreAuthorize("@domainSecurityService.isStudentOrInstructorOrAdmin()")
    public ResponseEntity<ApiResponse<Boolean>> hasCapacityForEnrollment(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to check capacity for instance: {}", instanceUuid);

        boolean hasCapacity = timetableService.hasCapacityForEnrollment(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(hasCapacity,
            hasCapacity ? "Capacity available" : "Instance is at full capacity"));
    }
}
