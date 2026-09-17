package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationEventDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDecisionRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobEligibilityDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobServiceInterface;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingConflictException;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(ClassMarketplaceJobController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Marketplace Jobs", description = "Marketplace class adverts posted by organisations, and the recruitment funnel that ends in a hire")
public class ClassMarketplaceJobController {

    public static final String API_ROOT_PATH = "/api/v1/classes/jobs";

    private final ClassMarketplaceJobServiceInterface classMarketplaceJobService;

    @Operation(summary = "Create a marketplace class job",
            description = "Attached resources are validated against their calendars and reserved with HOLD bookings for every session occurrence; conflicts return 409 with a per-occurrence report. A preferred instructor whose schedule clashes with the sessions is not hired: 409 with the clashing windows, and nothing is posted. A preferred instructor with no approved rate for the job's format, delivery and rate_basis, or a rate above instructor_pay, is refused with 409")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassMarketplaceJobDTO>> createJob(
            @Valid @RequestBody ClassMarketplaceJobRequestDTO request) {
        try {
            ClassMarketplaceJobDTO result = classMarketplaceJobService.createJob(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(result, "Marketplace class job created successfully"));
        } catch (ResourceBookingConflictException e) {
            log.warn("Resource conflicts while creating marketplace class job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Resource conflicts detected", e.getReport().conflicts()));
        } catch (SchedulingConflictException e) {
            log.warn("Preferred instructor clashes with the new marketplace class job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Schedule conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "Upload a marketplace class job thumbnail",
            description = "Attaches a thumbnail image to an organisation's class advert. Returns the updated job with a resolved thumbnail_url.")
    @PostMapping(value = "/{uuid}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClassMarketplaceJobDTO>> uploadJobThumbnail(
            @PathVariable UUID uuid,
            @RequestParam("thumbnail") MultipartFile thumbnail) {
        ClassMarketplaceJobDTO result = classMarketplaceJobService.uploadJobThumbnail(uuid, thumbnail);
        return ResponseEntity.ok(ApiResponse.success(result, "Thumbnail uploaded successfully"));
    }

    @Operation(summary = "List marketplace class jobs",
            description = "instructor_pay is included only for admin-verified instructors, managers of the posting organisation and platform admins; other callers receive the advert without it")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<ClassMarketplaceJobDTO>>> listJobs(
            @RequestParam(value = "organisation_uuid", required = false) UUID organisationUuid,
            @RequestParam(value = "course_uuid", required = false) UUID courseUuid,
            @RequestParam(value = "program_uuid", required = false) UUID programUuid,
            @Parameter(description = "Only jobs delivered at this training branch")
            @RequestParam(value = "branch_uuid", required = false) UUID branchUuid,
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        Optional<ClassMarketplaceJobStatus> statusFilter = Optional.ofNullable(status)
                .filter(value -> !value.isBlank())
                .map(ClassMarketplaceJobStatus::fromValue);

        Page<ClassMarketplaceJobDTO> page = classMarketplaceJobService.listJobs(
                organisationUuid,
                courseUuid,
                programUuid,
                branchUuid,
                statusFilter.orElse(null),
                pageable
        );
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Marketplace class jobs retrieved successfully"));
    }

    @Operation(summary = "Get a marketplace class job",
            description = "instructor_pay is included only for admin-verified instructors, managers of the posting organisation and platform admins; other callers receive the advert without it")
    @GetMapping("/{jobUuid}")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobDTO>> getJob(@PathVariable UUID jobUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.getJob(jobUuid),
                "Marketplace class job retrieved successfully"
        ));
    }

    @Operation(summary = "Update a marketplace class job",
            description = "Existing resource holds are released and re-placed against the updated schedule; conflicts return 409 with a per-occurrence report and roll the update back")
    @PutMapping("/{jobUuid}")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobDTO>> updateJob(
            @PathVariable UUID jobUuid,
            @Valid @RequestBody ClassMarketplaceJobRequestDTO request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    classMarketplaceJobService.updateJob(jobUuid, request),
                    "Marketplace class job updated successfully"
            ));
        } catch (ResourceBookingConflictException e) {
            log.warn("Resource conflicts while updating marketplace class job {}: {}", jobUuid, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Resource conflicts detected", e.getReport().conflicts()));
        }
    }

    @Operation(summary = "Cancel a marketplace class job")
    @PostMapping("/{jobUuid}/cancel")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobDTO>> cancelJob(@PathVariable UUID jobUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.cancelJob(jobUuid),
                "Marketplace class job cancelled successfully"
        ));
    }

    @Operation(summary = "Apply to a marketplace class job",
            description = "Applications are hard-blocked (409 with conflict details) when the instructor's existing schedule overlaps any of the job's planned session occurrences, and refused with 409 when the instructor has no approved rate for the job's format, delivery and rate basis or that rate is above the job's pay")
    @PostMapping("/{jobUuid}/applications")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobApplicationDTO>> applyToJob(
            @PathVariable UUID jobUuid,
            @Valid @RequestBody(required = false) ClassMarketplaceJobApplicationRequestDTO request) {
        ClassMarketplaceJobApplicationRequestDTO payload =
                request != null ? request : new ClassMarketplaceJobApplicationRequestDTO(null);
        ClassMarketplaceJobApplicationDTO result;
        try {
            result = classMarketplaceJobService.applyToJob(jobUuid, payload);
        } catch (SchedulingConflictException e) {
            log.warn("Schedule conflicts while applying to marketplace class job {}: {}", jobUuid, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Schedule conflicts detected", e.getConflicts()));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Marketplace class job application submitted successfully"));
    }

    @Operation(summary = "Withdraw the current instructor's own marketplace class job application",
            description = "Allowed at any stage before assignment. A withdrawn application can be submitted again while the job is open.")
    @PostMapping("/{jobUuid}/applications/{applicationUuid}/withdraw")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobApplicationDTO>> withdrawApplication(
            @PathVariable UUID jobUuid,
            @PathVariable UUID applicationUuid,
            @Valid @RequestBody(required = false) ClassMarketplaceJobDecisionRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.withdrawApplication(jobUuid, applicationUuid, request),
                "Marketplace class job application withdrawn successfully"));
    }

    @Operation(summary = "Check the current instructor's eligibility for several marketplace class jobs",
            description = "One entry per known job, in request order, each shaped like the single eligibility read. Unknown job uuids are skipped. At most 50 job_uuids per call; more return 400. Callers without an instructor profile are refused")
    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<List<ClassMarketplaceJobEligibilityDTO>>> getJobsEligibility(
            @Parameter(description = "Comma-separated job uuids, at most 50")
            @RequestParam("job_uuids") List<UUID> jobUuids) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.getMyJobsEligibility(jobUuids),
                "Marketplace class job eligibility retrieved successfully"
        ));
    }

    @Operation(summary = "Check current instructor's eligibility for a marketplace class job",
            description = "eligible requires rate_ok: an approved rate for the job's format, delivery and rate basis that the job's pay covers")
    @GetMapping("/{jobUuid}/eligibility")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobEligibilityDTO>> getJobEligibility(@PathVariable UUID jobUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.getMyJobEligibility(jobUuid),
                "Marketplace class job eligibility retrieved successfully"
        ));
    }

    @Operation(summary = "List applications for a marketplace class job",
            description = "Restricted to managers of the organisation that posted the job, and to platform admins")
    @GetMapping("/{jobUuid}/applications")
    public ResponseEntity<ApiResponse<PagedDTO<ClassMarketplaceJobApplicationDTO>>> listJobApplications(
            @PathVariable UUID jobUuid,
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        Optional<ClassMarketplaceJobApplicationStatus> statusFilter = Optional.ofNullable(status)
                .filter(value -> !value.isBlank())
                .map(ClassMarketplaceJobApplicationStatus::fromValue);

        Page<ClassMarketplaceJobApplicationDTO> page = classMarketplaceJobService.listJobApplications(
                jobUuid,
                statusFilter.orElse(null),
                pageable
        );
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Marketplace class job applications retrieved successfully"));
    }

    @Operation(summary = "List my marketplace class job applications")
    @GetMapping("/applications/mine")
    public ResponseEntity<ApiResponse<PagedDTO<ClassMarketplaceJobApplicationDTO>>> listMyApplications(
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        Optional<ClassMarketplaceJobApplicationStatus> statusFilter = Optional.ofNullable(status)
                .filter(value -> !value.isBlank())
                .map(ClassMarketplaceJobApplicationStatus::fromValue);

        Page<ClassMarketplaceJobApplicationDTO> page = classMarketplaceJobService.listMyApplications(
                statusFilter.orElse(null),
                pageable
        );
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Marketplace class job applications retrieved successfully"));
    }

    @Operation(summary = "List marketplace class job applications for an instructor",
            description = "The instructor and platform admins see every application; an organisation manager sees only those made to their organisation's jobs; anyone else receives an empty page")
    @GetMapping("/applications/instructor/{instructorUuid}")
    public ResponseEntity<ApiResponse<PagedDTO<ClassMarketplaceJobApplicationDTO>>> listInstructorApplications(
            @PathVariable UUID instructorUuid,
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        Optional<ClassMarketplaceJobApplicationStatus> statusFilter = Optional.ofNullable(status)
                .filter(value -> !value.isBlank())
                .map(ClassMarketplaceJobApplicationStatus::fromValue);

        Page<ClassMarketplaceJobApplicationDTO> page = classMarketplaceJobService.listInstructorApplications(
                instructorUuid,
                statusFilter.orElse(null),
                pageable
        );
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Marketplace class job applications retrieved successfully"));
    }

    @Operation(summary = "Get one marketplace class job application",
            description = "Readable by the applicant instructor, managers of the organisation that posted the job, and platform admins; anyone else is refused with 403")
    @GetMapping("/{jobUuid}/applications/{applicationUuid}")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobApplicationDTO>> getJobApplication(
            @PathVariable UUID jobUuid,
            @PathVariable UUID applicationUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.getJobApplication(jobUuid, applicationUuid),
                "Marketplace class job application retrieved successfully"));
    }

    @Operation(summary = "List a marketplace class job application's activity",
            description = "Every step the application has taken, newest first: applied, reapplied, shortlisted, interviewing (an interview invitation, with interview_at), offered, hired, assigned (the class was created), rejected, not_selected and withdrawn, each with its actor and note. Same access as reading the application")
    @GetMapping("/{jobUuid}/applications/{applicationUuid}/events")
    public ResponseEntity<ApiResponse<List<ClassMarketplaceJobApplicationEventDTO>>> listJobApplicationEvents(
            @PathVariable UUID jobUuid,
            @PathVariable UUID applicationUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.listApplicationEvents(jobUuid, applicationUuid),
                "Marketplace class job application activity retrieved successfully"));
    }

    @Operation(summary = "Move a marketplace class job application through the funnel",
            description = "Stages run applied -> shortlisted -> interviewing -> offered -> hired and no stage may be skipped; hire is the last decision, after which the job's class can be created. A hire whose sessions clash with the instructor's schedule is refused with 409 and the clashing windows, and the application, job and time holds are left as they were. A hire whose instructor has no approved rate for the job's rate basis, or a rate above the job's pay, is refused with 409 before anything is written")
    @PostMapping("/{jobUuid}/applications/{applicationUuid}")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobApplicationDTO>> reviewApplication(
            @PathVariable UUID jobUuid,
            @PathVariable UUID applicationUuid,
            @RequestParam("action") String action,
            @Valid @RequestBody(required = false) ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJobDecisionRequestDTO payload =
                request != null ? request : new ClassMarketplaceJobDecisionRequestDTO(null, null);

        ClassMarketplaceJobApplicationDTO result;
        try {
            result = switch (action.toLowerCase()) {
                case "hire" -> classMarketplaceJobService.hireApplication(jobUuid, applicationUuid, payload);
                case "reject" -> classMarketplaceJobService.rejectApplication(jobUuid, applicationUuid, payload);
                case "shortlist" -> classMarketplaceJobService.moveApplicationToStage(jobUuid, applicationUuid,
                        apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus.SHORTLISTED, payload);
                case "interview" -> classMarketplaceJobService.moveApplicationToStage(jobUuid, applicationUuid,
                        apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus.INTERVIEWING, payload);
                case "offer" -> classMarketplaceJobService.moveApplicationToStage(jobUuid, applicationUuid,
                        apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus.OFFERED, payload);
                default -> throw new IllegalArgumentException("Unsupported action '" + action
                        + "'. Allowed values: shortlist, interview, offer, hire, reject.");
            };
        } catch (SchedulingConflictException e) {
            log.warn("Schedule conflicts while hiring application {} on marketplace class job {}: {}",
                    applicationUuid, jobUuid, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Schedule conflicts detected", e.getConflicts()));
        }

        String message = switch (action.toLowerCase()) {
            case "hire" -> "Applicant hired successfully";
            case "reject" -> "Marketplace class job application rejected successfully";
            case "shortlist" -> "Candidate shortlisted successfully";
            case "interview" -> "Candidate moved to interview successfully";
            case "offer" -> "Offer extended to candidate successfully";
            default -> "Marketplace class job application updated successfully";
        };

        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @Operation(summary = "Create the class for a job whose applicant has been hired",
            description = "Creating the class is what assigns the hired instructor: it stamps their application assigned, converts their time holds and fills the job. There is no separate assign call. Refused with 409 when the hired instructor no longer has an approved rate for the job's rate basis that its pay covers.")
    @PostMapping("/{jobUuid}/class")
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> createClassForJob(@PathVariable UUID jobUuid) {
        try {
            ClassDefinitionDTO classDefinition = classMarketplaceJobService.createClassForJob(jobUuid);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(classDefinition, "Class created from marketplace job successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while creating the class for marketplace job {}: {}", jobUuid, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        } catch (ResourceBookingConflictException e) {
            log.warn("Resource conflicts while creating the class for marketplace job {}: {}", jobUuid, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Resource conflicts detected", e.getReport().conflicts()));
        }
    }
}
