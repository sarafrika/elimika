package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.timetabling.dto.BlockInstructorTimeRequest;
import apps.sarafrika.elimika.timetabling.security.TimetableSecurityService;
import apps.sarafrika.elimika.timetabling.service.SessionReportService;
import apps.sarafrika.elimika.timetabling.spi.ClassReportDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceRescheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionRecordDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionReportDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import apps.sarafrika.elimika.timetabling.spi.SessionReportSummaryDTO;
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
@RequestMapping("/api/v1/timetable")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Timetable API", description = "Class scheduling and timetable management")
@PreAuthorize("@domainSecurityService.isInstructorOrAdmin()")
public class TimetableController {

    /** Changing a session belongs to whoever runs its class; the instructor class-level rule alone let anyone. */
    private static final String MANAGE_SESSION = "@enrollmentVisibilityService.canManageClassOfInstance(#instanceUuid)";
    /** Running a session (start, end, status) also belongs to the instructor scheduled to teach it. */
    private static final String RUN_SESSION = MANAGE_SESSION + " or @timetableSecurityService.teachesSession(#instanceUuid)";
    private static final String OWN_INSTRUCTOR =
            "@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()";
    private static final String READ_LEARNER_RECORD = "@enrollmentSecurityService.isOwner(#studentUuid, 'student') or "
            + "@timetableSecurityService.canReadLearnerRecord(#studentUuid)";

    private final TimetableService timetableService;
    private final SessionReportService sessionReportService;
    private final TimetableSecurityService timetableSecurityService;
    private final DomainSecurityService domainSecurityService;
    private final InstructorTimeHoldService instructorTimeHoldService;

    // ================================
    // SCHEDULING OPERATIONS
    // ================================

    @Operation(summary = "Schedule a new class instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class scheduled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data or scheduling conflict")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition or instructor not found")
    @PostMapping("/schedule")
    @PreAuthorize("@classAccessSecurityService.canManageClass(#request.classDefinitionUuid())")
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
    @PreAuthorize(MANAGE_SESSION)
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
    @PreAuthorize(RUN_SESSION)
    public ResponseEntity<ApiResponse<Void>> updateScheduledInstanceStatus(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid,
            @Parameter(description = "New status (SCHEDULED, ONGOING, COMPLETED, CANCELLED, BLOCKED)")
            @RequestParam String status) {
        log.debug("REST request to update status of scheduled instance: {} to: {}", instanceUuid, status);
        
        timetableService.updateScheduledInstanceStatus(instanceUuid, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Status updated successfully"));
    }

    @Operation(summary = "Set the lesson a scheduled class instance teaches",
            description = "Attendance for the session is graded on that lesson. Without one, the session's position "
                    + "in the class schedule picks the lesson with the same number.")
    @PreAuthorize("@enrollmentVisibilityService.canManageClassOfInstance(#instanceUuid)")
    @PatchMapping("/schedule/{instanceUuid}/lesson")
    public ResponseEntity<ApiResponse<Void>> assignScheduledInstanceLesson(
            @PathVariable UUID instanceUuid,
            @Parameter(description = "Lesson UUID; omit to clear")
            @RequestParam(required = false) UUID lessonUuid) {
        timetableService.assignLesson(instanceUuid, lessonUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Session lesson updated successfully"));
    }

    @Operation(summary = "Get a session's report",
            description = "The session's instructors get the live report with its checklist; the school's managers get the last submitted version.")
    @GetMapping("/schedule/{instanceUuid}/report")
    @PreAuthorize("@timetableSecurityService.isSessionInstructor(#instanceUuid) "
            + "or @timetableSecurityService.managesOrganisationOfSession(#instanceUuid)")
    public ResponseEntity<ApiResponse<SessionReportDTO>> getSessionReport(@PathVariable UUID instanceUuid) {
        SessionReportDTO report = timetableSecurityService.isSessionInstructor(instanceUuid)
                ? sessionReportService.preview(instanceUuid)
                : sessionReportService.submittedReport(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(report, "Session report retrieved successfully"));
    }

    @Operation(summary = "Submit a session's report",
            description = "Final submission: refused while anything on the checklist blocks it. Complete draft scores are submitted with it. Editable until the school's grace period ends.")
    @PostMapping("/schedule/{instanceUuid}/report/submit")
    @PreAuthorize("@timetableSecurityService.isSessionInstructor(#instanceUuid)")
    public ResponseEntity<ApiResponse<SessionReportDTO>> submitSessionReport(@PathVariable UUID instanceUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionReportService.submit(instanceUuid, domainSecurityService.getCurrentUserUuid()),
                "Session report submitted"));
    }

    @Operation(summary = "Reopen a locked session report", description = "Platform admins only: opens one more grace period.")
    @PostMapping("/schedule/{instanceUuid}/report/reopen")
    @PreAuthorize("@domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<SessionReportDTO>> reopenSessionReport(@PathVariable UUID instanceUuid) {
        return ResponseEntity.ok(ApiResponse.success(sessionReportService.reopen(instanceUuid), "Session report reopened"));
    }

    @Operation(summary = "Get a class report", description = "Every learner's lesson marks across the class's sessions. School managers see submitted session reports only.")
    @GetMapping("/classes/{classDefinitionUuid}/report")
    @PreAuthorize("@classAccessSecurityService.canManageClass(#classDefinitionUuid)")
    public ResponseEntity<ApiResponse<ClassReportDTO>> getClassReport(@PathVariable UUID classDefinitionUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionReportService.classReport(classDefinitionUuid, timetableSecurityService.isClassInstructor(classDefinitionUuid)),
                "Class report retrieved successfully"));
    }

    @Operation(summary = "Download a class report as CSV")
    @GetMapping(value = "/classes/{classDefinitionUuid}/report.csv", produces = "text/csv")
    @PreAuthorize("@classAccessSecurityService.canManageClass(#classDefinitionUuid)")
    public ResponseEntity<String> getClassReportCsv(@PathVariable UUID classDefinitionUuid) {
        String csv = sessionReportService.classReportCsv(classDefinitionUuid, timetableSecurityService.isClassInstructor(classDefinitionUuid));
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"class-report-" + classDefinitionUuid + ".csv\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @Operation(summary = "List my session reports", description = "The signed-in instructor's session reports, soonest due first.")
    @GetMapping("/session-reports")
    @PreAuthorize("@domainSecurityService.getCurrentInstructorUuid() != null")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<SessionReportSummaryDTO>>> getMySessionReports(
            @RequestParam(required = false) List<String> status,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(sessionReportService.listForInstructor(
                domainSecurityService.getCurrentInstructorUuid(), SessionReportStatus.parseAll(status), pageable), "Session reports retrieved successfully"));
    }

    @Operation(summary = "Get what was taught in a session", description = "Topic covered, instructor remarks and duration.")
    @GetMapping("/schedule/{instanceUuid}/record")
    @PreAuthorize("@timetableSecurityService.canReadInstanceRoster(#instanceUuid)")
    public ResponseEntity<ApiResponse<SessionRecordDTO>> getSessionRecord(@PathVariable UUID instanceUuid) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getSessionRecord(instanceUuid),
                "Session record retrieved successfully"));
    }

    @Operation(summary = "Record what was taught in a session", description = "Saves the topic covered and the instructor's remarks.")
    @PutMapping("/schedule/{instanceUuid}/record")
    @PreAuthorize(RUN_SESSION)
    public ResponseEntity<ApiResponse<SessionRecordDTO>> recordSession(
            @PathVariable UUID instanceUuid,
            @Valid @RequestBody SessionRecordDTO record) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.recordSession(instanceUuid, record.topicCovered(), record.instructorRemarks()),
                "Session record saved successfully"));
    }

    @Operation(summary = "Reschedule a scheduled class instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scheduled instance rescheduled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid time range, scheduling conflict, or instance is not reschedulable")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Linked venue or equipment is not available at the new time")
    @PatchMapping("/schedule/{instanceUuid}/time")
    @PreAuthorize(MANAGE_SESSION)
    public ResponseEntity<ApiResponse<ScheduledInstanceDTO>> rescheduleScheduledInstance(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid,
            @Valid @RequestBody ScheduledInstanceRescheduleRequestDTO request) {
        log.debug("REST request to reschedule scheduled instance: {}", instanceUuid);

        try {
            ScheduledInstanceDTO result = timetableService.rescheduleScheduledInstance(instanceUuid, request);
            return ResponseEntity.ok(ApiResponse.success(result, "Scheduled instance rescheduled successfully"));
        } catch (apps.sarafrika.elimika.resourcing.spi.ResourceBookingConflictException e) {
            log.warn("Resource conflicts while rescheduling instance {}: {}", instanceUuid, e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Resource conflicts detected", e.getReport().conflicts()));
        }
    }

    @Operation(summary = "Start a scheduled class instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scheduled instance started successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Scheduled instance cannot be started")
    @PostMapping("/schedule/{instanceUuid}/start")
    @PreAuthorize(RUN_SESSION)
    public ResponseEntity<ApiResponse<ScheduledInstanceDTO>> startScheduledInstance(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to start scheduled instance: {}", instanceUuid);

        ScheduledInstanceDTO result = timetableService.startScheduledInstance(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class started successfully"));
    }

    @Operation(summary = "End a scheduled class instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scheduled instance ended successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Scheduled instance cannot be ended")
    @PostMapping("/schedule/{instanceUuid}/end")
    @PreAuthorize(RUN_SESSION)
    public ResponseEntity<ApiResponse<ScheduledInstanceDTO>> endScheduledInstance(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to end scheduled instance: {}", instanceUuid);

        ScheduledInstanceDTO result = timetableService.endScheduledInstance(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class ended successfully"));
    }

    // ================================
    // SCHEDULE QUERIES
    // ================================

    @Operation(summary = "Get a scheduled instance by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scheduled instance retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Scheduled instance not found")
    @GetMapping("/schedule/{instanceUuid}")
    @PreAuthorize("@timetableSecurityService.canReadInstanceRoster(#instanceUuid)")
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

    @Operation(summary = "Get marketplace time holds for a specific instructor within a date range",
            description = "Tentative and firm claims raised by the instructor's marketplace job applications. "
                    + "Read separately from the schedule because a hold is not a session: it carries no enrolment, "
                    + "attendance or pay, and only a FIRM hold counts as a scheduling clash.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Instructor time holds retrieved successfully")
    @GetMapping("/instructors/{instructorUuid}/time-holds")
    // Narrower than the schedule endpoint on purpose: a hold names the organisations an instructor
    // has applied to while the decision is pending, so the self-service instructor domain alone
    // would let any competitor read another instructor's in-flight job search.
    @PreAuthorize("@domainSecurityService.isInstructorWithUuid(#instructorUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<List<InstructorTimeHoldDTO>>> getInstructorTimeHolds(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Start date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.debug("REST request to get time holds for instructor: {} from {} to {}", instructorUuid, start, end);

        List<InstructorTimeHoldDTO> result = instructorTimeHoldService.findActiveHolds(instructorUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(result, "Instructor time holds retrieved successfully"));
    }

    @Operation(summary = "Get schedule for a specific student within a date range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student schedule retrieved successfully")
    @GetMapping("/student/{studentUuid}")
    @PreAuthorize(READ_LEARNER_RECORD)
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

    @Operation(summary = "Block instructor calendar for non-teaching time")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Block created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid block request")
    @PostMapping("/instructor/{instructorUuid}/blocks")
    @PreAuthorize(OWN_INSTRUCTOR)
    public ResponseEntity<ApiResponse<ScheduledInstanceDTO>> blockInstructorTime(
            @Parameter(description = "UUID of the instructor")
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody BlockInstructorTimeRequest request) {
        int periodCount = request.periods() != null ? request.periods().size() : 0;
        log.debug("REST request to block instructor {} for {} period(s)", instructorUuid, periodCount);

        ScheduledInstanceDTO result = timetableService.blockInstructorTime(instructorUuid, request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Instructor time blocked"));
    }

    @Operation(summary = "Check if a student has enrollment conflicts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conflict check completed")
    @PostMapping("/student/{studentUuid}/check-conflict")
    @PreAuthorize(READ_LEARNER_RECORD)
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
