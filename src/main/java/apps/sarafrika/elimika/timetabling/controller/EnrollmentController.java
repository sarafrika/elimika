package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.timetabling.spi.OrganisationStudentPerformanceDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrolmentTrendPointDTO;
import apps.sarafrika.elimika.timetabling.spi.TodayGrowthPointDTO;
import apps.sarafrika.elimika.timetabling.spi.WeeklyGrowthPointDTO;
import apps.sarafrika.elimika.timetabling.spi.OrganisationActivityEventDTO;
import apps.sarafrika.elimika.timetabling.spi.ClassEnrolmentCountDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentEnrolmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.ClassEnrolmentEligibilityDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentVisibilityService;
import apps.sarafrika.elimika.timetabling.spi.StudentCourseEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentEnrollmentOverviewDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enrolments, attendance and the figures drawn from them.
 * <p>
 * Every row here names a learner, so the guards are asked against the class the row sits in rather
 * than against the caller's roles: the register of a class belongs to whoever runs that class.
 * Where the caller chooses the filter — the enrolment search — the rows cannot be settled in
 * advance, so they are filtered one by one instead of the request being refused outright, leaving
 * each caller the rows they are party to. Organisation-wide figures are for that organisation's
 * managers; belonging to the organisation as a learner or a contracted trainer is not enough.
 *
 * @see EnrollmentVisibilityService
 */
@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment API", description = "Student enrollment and attendance management")
public class EnrollmentController {

    /**
     * Every organisation-scoped route below answers a question about one organisation's learners,
     * classes or money, so each is confined to the people who run that organisation — an org-scoped
     * {@code organisation_user} or {@code admin} of it — plus platform admins doing support. The
     * check is against the organisation named in the path, never a role held somewhere else.
     */
    private static final String MANAGE_ORGANISATION =
            "@domainSecurityService.isPlatformAdmin() or @domainSecurityService.managesOrganisation(#organisationUuid)";

    /**
     * A session's roster is the list of learners sitting in one organisation's class, so reaching it
     * takes a relationship to that session — teaching it, owning the class, or running the
     * organisation behind it — not merely holding the {@code instructor} or {@code admin} domain
     * somewhere on the platform.
     */
    private static final String READ_INSTANCE_ROSTER =
            "@timetableSecurityService.canReadInstanceRoster(#instanceUuid)";

    /**
     * The same reach, expressed over a single enrolment: the caller must hold the session the
     * enrolment sits on. Routes a learner may also use spell out their own ownership alongside it.
     */
    private static final String ACCESS_ENROLMENT =
            "@timetableSecurityService.canAccessEnrolment(#enrollmentUuid)";

    /**
     * A learner's enrolment record is theirs. It also belongs, in part, to the institutions they
     * joined — so those who share a session with them may read it, and no one else. Holding the
     * {@code instructor} domain elsewhere on the platform is not a relationship with this learner.
     */
    private static final String READ_LEARNER_RECORD =
            "@enrollmentSecurityService.isOwner(#studentUuid, 'student') or "
                    + "@timetableSecurityService.canReadLearnerRecord(#studentUuid)";

    /**
     * Eligibility carries the learner's age and whether their date of birth is on file, so it is a
     * fact about one named person and cannot be gated on a domain the caller happens to hold
     * somewhere on the platform. It is asked before any enrolment exists, though, so a shared session
     * is not available to scope it either; the class in the path is. Either the learner is asking
     * about themselves, or the caller holds the class they are being signed up to — its instructor,
     * a manager of the owning organisation, or a platform administrator.
     */
    private static final String CHECK_ELIGIBILITY =
            "@enrollmentSecurityService.isOwner(#studentUuid, 'student') or "
                    + "@timetableSecurityService.canReadClassRoster(#classDefinitionUuid)";

    private final TimetableService timetableService;
    private final EnrollmentVisibilityService enrollmentVisibilityService;

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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller neither owns the enrolment nor holds its session")
    @DeleteMapping("/{enrollmentUuid}")
    // Two units guarded this route. canManageClassOfEnrollment is the narrower of the two: it
    // admits the class's own instructor, a manager of the owning organisation and platform admins,
    // but not whoever merely happens to be scheduled on one sitting of the class -- any instructor
    // can schedule an instance through the timetabling API, so that would be self-grantable.
    // ACCESS_ENROLMENT is the wider reading of the same rule; the narrower one is enforced.
    @PreAuthorize("@enrollmentSecurityService.isOwner(#enrollmentUuid) "
            + "or @enrollmentVisibilityService.canManageClassOfEnrollment(#enrollmentUuid)")
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not hold the session this enrolment sits on")
    @PatchMapping("/{enrollmentUuid}/attendance")
    // See cancelEnrollment: the narrower of the two guards proposed for this route is enforced.
    @PreAuthorize("@enrollmentVisibilityService.canManageClassOfEnrollment(#enrollmentUuid)")
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller neither owns the enrolment nor holds its session")
    @GetMapping("/{enrollmentUuid}")
    // Two units guarded this route. canManageClassOfEnrollment is the narrower of the two: it
    // admits the class's own instructor, a manager of the owning organisation and platform admins,
    // but not whoever merely happens to be scheduled on one sitting of the class -- any instructor
    // can schedule an instance through the timetabling API, so that would be self-grantable.
    // ACCESS_ENROLMENT is the wider reading of the same rule; the narrower one is enforced.
    @PreAuthorize("@enrollmentSecurityService.isOwner(#enrollmentUuid) "
            + "or @enrollmentVisibilityService.canManageClassOfEnrollment(#enrollmentUuid)")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> getEnrollment(
            @Parameter(description = "UUID of the enrollment to retrieve")
            @PathVariable UUID enrollmentUuid) {
        log.debug("REST request to get enrollment: {}", enrollmentUuid);

        EnrollmentDTO result = timetableService.getEnrollment(enrollmentUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Enrollment retrieved successfully"));
    }

    @Operation(
            summary = "Get all enrollments for a scheduled instance",
            description = "The learners on one session's roster, with their enrolment status. Reserved "
                    + "for the people who hold that session — the instructor teaching it, the owner of "
                    + "the class, or a manager of the organisation behind it — and platform administrators."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not hold this session")
    @GetMapping("/instance/{instanceUuid}")
    // Narrower than READ_INSTANCE_ROSTER by design: running the class the session belongs to,
    // rather than being scheduled on the session, which an instructor can arrange for themselves.
    @PreAuthorize("@enrollmentVisibilityService.canManageClassOfInstance(#instanceUuid)")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getEnrollmentsForInstance(
            @Parameter(description = "UUID of the scheduled instance")
            @PathVariable UUID instanceUuid) {
        log.debug("REST request to get enrollments for scheduled instance: {}", instanceUuid);

        List<EnrollmentDTO> result = timetableService.getEnrollmentsForInstance(instanceUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Search enrollments",
            description = "Search enrollments using query parameters such as student_uuid and class_definition_uuid. "
                    + "The filter is the caller's to choose, so the result is confined to the rows they are party "
                    + "to: enrolments in a class they run -- their own classes and those of organisations they "
                    + "manage -- and their own enrolments. Platform administrators are unrestricted. Rows in "
                    + "anyone else's class are not returned and are not counted in the total, since a filter "
                    + "answered over withheld rows would disclose them just as plainly."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment search completed successfully")
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedDTO<EnrollmentDTO>>> searchEnrollments(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        log.debug("REST request to search enrollments with params: {}", searchParams);

        Page<EnrollmentDTO> results = enrollmentVisibilityService.visibleToCaller(
                timetableService.searchEnrollments(searchParams, pageable));
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(results, baseUrl), "Enrollment search completed successfully"));
    }

    @Operation(summary = "Get scheduled instance enrollments for a specific student")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student scheduled instance enrollments retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller shares no session with this learner")
    @GetMapping("/student/{studentUuid}/scheduled-instances")
    @PreAuthorize(READ_LEARNER_RECORD)
    public ResponseEntity<ApiResponse<PagedDTO<EnrollmentDTO>>> getScheduledInstanceEnrollmentsForStudent(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            Pageable pageable) {
        log.debug("REST request to get scheduled instance enrollments for student: {}", studentUuid);

        Page<EnrollmentDTO> result = timetableService.getEnrollmentsForStudent(studentUuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(result, baseUrl),
                "Student scheduled instance enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Check whether a student may join a class before they pay for it",
            description = "Answers yes or no for one learner against one class. It is asked before the "
                    + "enrolment exists, so a shared session cannot be required; the reach is over the "
                    + "class instead — the learner themselves, or whoever holds the class they are being "
                    + "signed up to."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Eligibility resolved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is neither the learner nor a holder of this class")
    @GetMapping("/eligibility/{classDefinitionUuid}/student/{studentUuid}")
    @PreAuthorize(CHECK_ELIGIBILITY)
    public ResponseEntity<ApiResponse<ClassEnrolmentEligibilityDTO>> getClassEnrolmentEligibility(
            @Parameter(description = "UUID of the class definition")
            @PathVariable UUID classDefinitionUuid,
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid) {
        log.debug("REST request for enrolment eligibility: student {} on class {}", studentUuid, classDefinitionUuid);

        ClassEnrolmentEligibilityDTO eligibility =
                timetableService.getClassEnrolmentEligibility(classDefinitionUuid, studentUuid);
        return ResponseEntity.ok(ApiResponse.success(eligibility, "Enrolment eligibility resolved"));
    }

    @Operation(summary = "Get class enrollments for a specific student")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student class enrollments retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller shares no session with this learner")
    @GetMapping("/student/{studentUuid}/classes")
    @PreAuthorize(READ_LEARNER_RECORD)
    public ResponseEntity<ApiResponse<PagedDTO<StudentClassEnrollmentSummaryDTO>>> getClassEnrollmentsForStudent(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            Pageable pageable) {
        log.debug("REST request to get class enrollments for student: {}", studentUuid);

        Page<StudentClassEnrollmentSummaryDTO> result = timetableService.getClassEnrollmentsForStudent(studentUuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(result, baseUrl),
                "Student class enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Get course enrollments for a specific student",
            description = "Returns the student's course progress across the whole platform, so it is " +
                    "restricted to the student themselves and platform administrators. An " +
                    "organisation or instructor wanting to see how a student is doing at their own " +
                    "institution must use the organisation-scoped performance endpoint instead, " +
                    "which cannot reach beyond that institution's classes."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student course enrollments retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is neither the student nor a platform administrator")
    @GetMapping("/student/{studentUuid}/courses")
    @PreAuthorize("@enrollmentSecurityService.isOwner(#studentUuid, 'student') or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<PagedDTO<StudentCourseEnrollmentSummaryDTO>>> getCourseEnrollmentsForStudent(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            Pageable pageable) {
        log.debug("REST request to get course enrollments for student: {}", studentUuid);

        Page<StudentCourseEnrollmentSummaryDTO> result = timetableService.getCourseEnrollmentsForStudent(studentUuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(result, baseUrl),
                "Student course enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Get overall student enrollment overview",
            description = "Retrieves overall class and course enrollments for a student without requiring " +
                    "scheduled-instance inspection. Composing two views does not widen either of them: the " +
                    "course-progress half is the platform-wide record, so it is filled in only for the " +
                    "student themselves and platform administrators, exactly as the /courses route allows. " +
                    "Anyone else sees the class half and an empty course half."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student enrollment overview retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller shares no session with this learner")
    @GetMapping("/student/{studentUuid}/overview")
    @PreAuthorize(READ_LEARNER_RECORD)
    public ResponseEntity<ApiResponse<StudentEnrollmentOverviewDTO>> getEnrollmentOverviewForStudent(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            Pageable pageable) {
        log.debug("REST request to get overall enrollment overview for student: {}", studentUuid);

        String studentEnrollmentBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/enrollment/student/{studentUuid}")
                .buildAndExpand(studentUuid)
                .toUriString();
        StudentEnrollmentOverviewDTO result = new StudentEnrollmentOverviewDTO(
                studentUuid,
                PagedDTO.from(
                        timetableService.getClassEnrollmentsForStudent(studentUuid, pageable),
                        studentEnrollmentBaseUrl + "/classes"),
                PagedDTO.from(
                        timetableService.getCourseEnrollmentsForStudent(studentUuid, pageable),
                        studentEnrollmentBaseUrl + "/courses")
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Student enrollment overview retrieved successfully"));
    }

    // ================================
    // CAPACITY AND STATISTICS
    // ================================

    @Operation(summary = "Get enrollment count for a scheduled instance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment count retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not hold this session")
    @GetMapping("/instance/{instanceUuid}/count")
    // Narrower than READ_INSTANCE_ROSTER by design: running the class the session belongs to,
    // rather than being scheduled on the session, which an instructor can arrange for themselves.
    @PreAuthorize("@enrollmentVisibilityService.canManageClassOfInstance(#instanceUuid)")
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

    @Operation(
            summary = "Get organisation enrolment trends",
            description = "Monthly enrolment counts across all classes owned by the organisation, oldest month first."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrolment trends retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/enrolment-trends")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<EnrolmentTrendPointDTO>>> getEnrolmentTrends(
            @Parameter(description = "UUID of the organisation to scope the trend to")
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Number of months to include (inclusive of the current month)")
            @RequestParam(defaultValue = "6") int months) {
        log.debug("REST request for enrolment trends of organisation {} over {} months", organisationUuid, months);

        List<EnrolmentTrendPointDTO> trends =
                timetableService.getEnrolmentTrendsForOrganisation(organisationUuid, months);
        return ResponseEntity.ok(ApiResponse.success(trends, "Enrolment trends retrieved successfully"));
    }

    @Operation(
            summary = "Get organisation today's-growth",
            description = "Hourly enrolment counts for the current day across all classes owned by the organisation."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Today's growth retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/today-growth")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<TodayGrowthPointDTO>>> getTodayGrowth(
            @Parameter(description = "UUID of the organisation to scope to")
            @PathVariable UUID organisationUuid) {
        log.debug("REST request for today's growth of organisation {}", organisationUuid);

        List<TodayGrowthPointDTO> growth = timetableService.getTodayGrowthForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(growth, "Today's growth retrieved successfully"));
    }

    @Operation(
            summary = "Get organisation weekly-growth",
            description = "Distinct students-per-course enrolled in each ISO week over the requested span, " +
                    "across all classes owned by the organisation, oldest week first."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly growth retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/weekly-growth")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<WeeklyGrowthPointDTO>>> getWeeklyGrowth(
            @Parameter(description = "UUID of the organisation to scope to")
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Number of ISO weeks to include (inclusive of the current week)")
            @RequestParam(defaultValue = "8") int weeks) {
        log.debug("REST request for weekly growth of organisation {} over {} weeks", organisationUuid, weeks);

        List<WeeklyGrowthPointDTO> growth = timetableService.getWeeklyGrowthForOrganisation(organisationUuid, weeks);
        return ResponseEntity.ok(ApiResponse.success(growth, "Weekly growth retrieved successfully"));
    }

    @Operation(
            summary = "Get per-class enrolment counts for an organisation",
            description = "Distinct active-enrolment counts for each class definition the organisation owns."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class enrolment counts retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/class-enrolment-counts")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<ClassEnrolmentCountDTO>>> getClassEnrolmentCounts(
            @Parameter(description = "UUID of the organisation to scope to")
            @PathVariable UUID organisationUuid) {
        log.debug("REST request for per-class enrolment counts of organisation {}", organisationUuid);

        List<ClassEnrolmentCountDTO> counts =
                timetableService.getClassEnrolmentCountsForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(counts, "Class enrolment counts retrieved successfully"));
    }

    @Operation(
            summary = "Get an organisation's activity feed",
            description = "Recent, human-meaningful events across the organisation — students enrolling, "
                    + "classes being opened and instructors being paid — newest first. The amount and "
                    + "currency on PAYOUT events are disclosed only to those who manage the organisation."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity feed retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/activity-feed")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<OrganisationActivityEventDTO>>> getActivityFeed(
            @Parameter(description = "UUID of the organisation to scope to")
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Maximum number of events to return (1-100)")
            @RequestParam(defaultValue = "20") int limit) {
        log.debug("REST request for activity feed of organisation {} (limit {})", organisationUuid, limit);

        List<OrganisationActivityEventDTO> events =
                timetableService.getActivityFeedForOrganisation(organisationUuid, limit);
        return ResponseEntity.ok(ApiResponse.success(events, "Activity feed retrieved successfully"));
    }

    @Operation(
            summary = "Get per-student enrolment summaries for an organisation",
            description = "Per-student total and attended (completed) enrolment counts across the organisation's classes."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student enrolment summaries retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/student-summaries")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<StudentEnrolmentSummaryDTO>>> getStudentSummaries(
            @Parameter(description = "UUID of the organisation to scope to")
            @PathVariable UUID organisationUuid) {
        log.debug("REST request for per-student enrolment summaries of organisation {}", organisationUuid);

        List<StudentEnrolmentSummaryDTO> summaries =
                timetableService.getStudentEnrolmentSummariesForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(summaries, "Student enrolment summaries retrieved successfully"));
    }

    @Operation(
            summary = "Get one student's performance within an organisation",
            description = "Per-class attendance and performance for a single student, confined to the " +
                    "organisation's own classes. An organisation may only see how a student is doing " +
                    "at its own institution; their learning elsewhere on the platform is unreachable " +
                    "through this endpoint by construction, not by filtering afterwards. Only those " +
                    "who manage the organisation may ask; being a fellow member of it is not enough."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student performance retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping("/organisations/{organisationUuid}/students/{studentUuid}/performance")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<OrganisationStudentPerformanceDTO>>> getStudentPerformance(
            @Parameter(description = "UUID of the organisation to scope to")
            @PathVariable UUID organisationUuid,
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid) {
        log.debug("REST request for performance of student {} within organisation {}", studentUuid, organisationUuid);

        List<OrganisationStudentPerformanceDTO> performance =
                timetableService.getStudentPerformanceForOrganisation(organisationUuid, studentUuid);
        return ResponseEntity.ok(ApiResponse.success(performance, "Student performance retrieved successfully"));
    }
}
