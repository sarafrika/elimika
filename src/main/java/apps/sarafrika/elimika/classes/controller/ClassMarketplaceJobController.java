package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobApplicationRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentResponseDTO;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(ClassMarketplaceJobController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Marketplace Jobs", description = "Marketplace class adverts posted by organisations before a final instructor is assigned")
public class ClassMarketplaceJobController {

    public static final String API_ROOT_PATH = "/api/v1/classes/jobs";

    private final ClassMarketplaceJobServiceInterface classMarketplaceJobService;

    @Operation(summary = "Create a marketplace class job",
            description = "Attached resources are validated against their calendars and reserved with HOLD bookings for every session occurrence; conflicts return 409 with a per-occurrence report")
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
        }
    }

    @Operation(summary = "List marketplace class jobs")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<ClassMarketplaceJobDTO>>> listJobs(
            @RequestParam(value = "organisation_uuid", required = false) UUID organisationUuid,
            @RequestParam(value = "course_uuid", required = false) UUID courseUuid,
            @RequestParam(value = "program_uuid", required = false) UUID programUuid,
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {
        Optional<ClassMarketplaceJobStatus> statusFilter = Optional.ofNullable(status)
                .filter(value -> !value.isBlank())
                .map(ClassMarketplaceJobStatus::fromValue);

        Page<ClassMarketplaceJobDTO> page = classMarketplaceJobService.listJobs(
                organisationUuid,
                courseUuid,
                programUuid,
                statusFilter.orElse(null),
                pageable
        );
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Marketplace class jobs retrieved successfully"));
    }

    @Operation(summary = "Get a marketplace class job")
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
            description = "Applications are hard-blocked (409 with conflict details) when the instructor's existing schedule overlaps any of the job's planned session occurrences")
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

    @Operation(summary = "Check current instructor's eligibility for a marketplace class job")
    @GetMapping("/{jobUuid}/eligibility")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobEligibilityDTO>> getJobEligibility(@PathVariable UUID jobUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                classMarketplaceJobService.getMyJobEligibility(jobUuid),
                "Marketplace class job eligibility retrieved successfully"
        ));
    }

    @Operation(summary = "List applications for a marketplace class job")
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

    @Operation(summary = "List marketplace class job applications for an instructor")
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

    @Operation(summary = "Approve or reject a marketplace class job application")
    @PostMapping("/{jobUuid}/applications/{applicationUuid}")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobApplicationDTO>> reviewApplication(
            @PathVariable UUID jobUuid,
            @PathVariable UUID applicationUuid,
            @RequestParam("action") String action,
            @Valid @RequestBody(required = false) ClassMarketplaceJobDecisionRequestDTO request) {
        ClassMarketplaceJobDecisionRequestDTO payload =
                request != null ? request : new ClassMarketplaceJobDecisionRequestDTO(null);

        ClassMarketplaceJobApplicationDTO result = switch (action.toLowerCase()) {
            case "approve" -> classMarketplaceJobService.approveApplication(jobUuid, applicationUuid, payload);
            case "reject" -> classMarketplaceJobService.rejectApplication(jobUuid, applicationUuid, payload);
            default -> throw new IllegalArgumentException("Unsupported action '" + action + "'. Allowed values: approve, reject.");
        };

        String message = switch (action.toLowerCase()) {
            case "approve" -> "Marketplace class job application approved successfully";
            case "reject" -> "Marketplace class job application rejected successfully";
            default -> "Marketplace class job application updated successfully";
        };

        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @Operation(summary = "Assign an approved instructor and create the actual class")
    @PostMapping("/{jobUuid}/assignments")
    public ResponseEntity<ApiResponse<ClassMarketplaceJobAssignmentResponseDTO>> assignInstructor(
            @PathVariable UUID jobUuid,
            @Valid @RequestBody ClassMarketplaceJobAssignmentRequestDTO request) {
        try {
            ClassMarketplaceJobAssignmentResponseDTO result = classMarketplaceJobService.assignInstructor(jobUuid, request);
            return ResponseEntity.ok(ApiResponse.success(result, "Marketplace class job assigned successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while assigning marketplace class job {}: {}", jobUuid, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }
}
