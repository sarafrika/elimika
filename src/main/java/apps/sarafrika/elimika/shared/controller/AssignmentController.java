package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.internal.AssignmentMediaValidationService;
import apps.sarafrika.elimika.course.service.*;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for comprehensive assignment and submission management.
 */
@RestController
@RequestMapping(AssignmentController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Assignment Management", description = "Complete assignment lifecycle including submissions, grading, and analytics")
public class AssignmentController {

    public static final String API_ROOT_PATH = "/api/v1/assignments";

    private final AssignmentService assignmentService;
    private final AssignmentSubmissionService assignmentSubmissionService;
    private final AssignmentAttachmentService assignmentAttachmentService;
    private final AssignmentSubmissionAttachmentService assignmentSubmissionAttachmentService;
    private final AssignmentMediaValidationService assignmentMediaValidationService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    // ===== ASSIGNMENT BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new assignment",
            description = "Creates a new assignment with unpublished state by default.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Assignment created successfully",
                            content = @Content(schema = @Schema(implementation = AssignmentDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentDTO>> createAssignment(
            @Valid @RequestBody AssignmentDTO assignmentDTO) {
        AssignmentDTO createdAssignment = assignmentService.createAssignment(assignmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdAssignment, "Assignment created successfully"));
    }

    @Operation(
            summary = "Get assignment by UUID",
            description = "Retrieves a complete assignment including submission statistics.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Assignment found"),
                    @ApiResponse(responseCode = "404", description = "Assignment not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentDTO>> getAssignmentByUuid(
            @PathVariable UUID uuid) {
        AssignmentDTO assignmentDTO = assignmentService.getAssignmentByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(assignmentDTO, "Assignment retrieved successfully"));
    }

    @Operation(
            summary = "Get all assignments",
            description = "Retrieves paginated list of all assignments with filtering support."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<AssignmentDTO>>> getAllAssignments(
            Pageable pageable) {
        Page<AssignmentDTO> assignments = assignmentService.getAllAssignments(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(assignments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Assignments retrieved successfully"));
    }

    @Operation(
            summary = "Update assignment",
            description = "Updates an existing assignment with selective field updates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Assignment updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Assignment not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentDTO>> updateAssignment(
            @PathVariable UUID uuid,
            @Valid @RequestBody AssignmentDTO assignmentDTO) {
        AssignmentDTO updatedAssignment = assignmentService.updateAssignment(uuid, assignmentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedAssignment, "Assignment updated successfully"));
    }

    @Operation(
            summary = "Delete assignment",
            description = "Permanently removes an assignment and all associated submissions.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Assignment deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Assignment not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable UUID uuid) {
        assignmentService.deleteAssignment(uuid);
        return ResponseEntity.noContent().build();
    }

    // ===== ASSIGNMENT ATTACHMENTS =====

    @Operation(
            summary = "Upload assignment attachment",
            description = """
                    Uploads an attachment for an assignment (documents, images, audio, or video).

                    **Use cases:**
                    - Providing assignment briefs as PDFs or slides
                    - Attaching sample datasets, images, or reference media
                    """
    )
    @PostMapping(
            value = "/{assignmentUuid}/attachments/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentAttachmentDTO>> uploadAssignmentAttachment(
            @PathVariable UUID assignmentUuid,
            @RequestParam("file") MultipartFile file
    ) {
        assignmentMediaValidationService.validateAssignmentAttachment(file);

        String folder = storageProperties.getFolders().getAssignments()
                + "/" + assignmentUuid
                + "/attachments";

        String storedFileName = storageService.store(file, folder);
        String fileUrl = storageProperties.getBaseUrl() != null
                ? storageProperties.getBaseUrl() + "/" + storedFileName
                : storedFileName;

        AssignmentAttachmentDTO requestDto = new AssignmentAttachmentDTO(
                null,
                assignmentUuid,
                file.getOriginalFilename(),
                storedFileName,
                fileUrl,
                file.getSize(),
                storageService.getContentType(storedFileName),
                null,
                null,
                null,
                null
        );

        AssignmentAttachmentDTO created = assignmentAttachmentService.createAttachment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(created, "Assignment attachment uploaded successfully"));
    }

    @Operation(
            summary = "Get assignment attachments",
            description = "Retrieves all attachments linked to a specific assignment."
    )
    @GetMapping("/{assignmentUuid}/attachments")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<AssignmentAttachmentDTO>>> getAssignmentAttachments(
            @PathVariable UUID assignmentUuid
    ) {
        List<AssignmentAttachmentDTO> attachments = assignmentAttachmentService.getAttachmentsByAssignment(assignmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(attachments, "Assignment attachments retrieved successfully"));
    }

    @Operation(
            summary = "Delete assignment attachment",
            description = "Removes a specific assignment attachment."
    )
    @DeleteMapping("/{assignmentUuid}/attachments/{attachmentUuid}")
    public ResponseEntity<Void> deleteAssignmentAttachment(
            @PathVariable UUID assignmentUuid,
            @PathVariable UUID attachmentUuid
    ) {
        assignmentAttachmentService.deleteAttachment(attachmentUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get assignment media by file name",
            description = "Retrieves assignment media files by their stored filename."
    )
    @GetMapping("/media/{fileName}")
    public ResponseEntity<Resource> getAssignmentMedia(
            @PathVariable String fileName
    ) {
        try {
            Resource resource = storageService.load(fileName);
            String contentType = storageService.getContentType(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== ASSIGNMENT SUBMISSIONS =====

    @Operation(
            summary = "Submit assignment",
            description = "Creates a new submission for an assignment by a student."
    )
    @PostMapping("/{assignmentUuid}/submit")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentSubmissionDTO>> submitAssignment(
            @PathVariable UUID assignmentUuid,
            @RequestParam UUID enrollmentUuid,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String[] fileUrls) {
        AssignmentSubmissionDTO submission = assignmentSubmissionService.submitAssignment(
                enrollmentUuid, assignmentUuid, content, fileUrls);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(submission, "Assignment submitted successfully"));
    }

    @Operation(
            summary = "Upload assignment submission attachment",
            description = "Uploads a file attachment for a specific assignment submission."
    )
    @PostMapping(
            value = "/{assignmentUuid}/submissions/{submissionUuid}/attachments/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentSubmissionAttachmentDTO>> uploadSubmissionAttachment(
            @PathVariable UUID assignmentUuid,
            @PathVariable UUID submissionUuid,
            @RequestParam("file") MultipartFile file
    ) {
        AssignmentSubmissionDTO submission = assignmentSubmissionService.getAssignmentSubmissionByUuid(submissionUuid);
        if (!assignmentUuid.equals(submission.assignmentUuid())) {
            throw new IllegalArgumentException("Submission does not belong to the specified assignment");
        }

        AssignmentDTO assignment = assignmentService.getAssignmentByUuid(assignmentUuid);
        assignmentMediaValidationService.validateSubmissionAttachment(assignment.submissionTypes(), file);

        String folder = storageProperties.getFolders().getAssignments()
                + "/" + assignmentUuid
                + "/submissions/" + submissionUuid;

        String storedFileName = storageService.store(file, folder);
        String fileUrl = storageProperties.getBaseUrl() != null
                ? storageProperties.getBaseUrl() + "/" + storedFileName
                : storedFileName;

        AssignmentSubmissionAttachmentDTO requestDto = new AssignmentSubmissionAttachmentDTO(
                null,
                submissionUuid,
                file.getOriginalFilename(),
                storedFileName,
                fileUrl,
                file.getSize(),
                storageService.getContentType(storedFileName),
                null,
                null,
                null,
                null
        );

        AssignmentSubmissionAttachmentDTO created = assignmentSubmissionAttachmentService.createAttachment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(created, "Submission attachment uploaded successfully"));
    }

    @Operation(
            summary = "Get submission attachments",
            description = "Retrieves all attachments for a specific assignment submission."
    )
    @GetMapping("/{assignmentUuid}/submissions/{submissionUuid}/attachments")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<AssignmentSubmissionAttachmentDTO>>> getSubmissionAttachments(
            @PathVariable UUID assignmentUuid,
            @PathVariable UUID submissionUuid
    ) {
        AssignmentSubmissionDTO submission = assignmentSubmissionService.getAssignmentSubmissionByUuid(submissionUuid);
        if (!assignmentUuid.equals(submission.assignmentUuid())) {
            throw new IllegalArgumentException("Submission does not belong to the specified assignment");
        }

        List<AssignmentSubmissionAttachmentDTO> attachments = assignmentSubmissionAttachmentService.getAttachmentsBySubmission(submissionUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(attachments, "Submission attachments retrieved successfully"));
    }

    @Operation(
            summary = "Delete submission attachment",
            description = "Removes a specific submission attachment."
    )
    @DeleteMapping("/{assignmentUuid}/submissions/{submissionUuid}/attachments/{attachmentUuid}")
    public ResponseEntity<Void> deleteSubmissionAttachment(
            @PathVariable UUID assignmentUuid,
            @PathVariable UUID submissionUuid,
            @PathVariable UUID attachmentUuid
    ) {
        AssignmentSubmissionDTO submission = assignmentSubmissionService.getAssignmentSubmissionByUuid(submissionUuid);
        if (!assignmentUuid.equals(submission.assignmentUuid())) {
            throw new IllegalArgumentException("Submission does not belong to the specified assignment");
        }

        assignmentSubmissionAttachmentService.deleteAttachment(attachmentUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get assignment submissions",
            description = "Retrieves all submissions for a specific assignment."
    )
    @GetMapping("/{assignmentUuid}/submissions")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<AssignmentSubmissionDTO>>> getAssignmentSubmissions(
            @PathVariable UUID assignmentUuid) {
        List<AssignmentSubmissionDTO> submissions = assignmentSubmissionService.getSubmissionsByAssignment(assignmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(submissions, "Assignment submissions retrieved successfully"));
    }

    @Operation(
            summary = "Grade submission",
            description = "Grades a student's assignment submission with score and comments."
    )
    @PostMapping("/{assignmentUuid}/submissions/{submissionUuid}/grade")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentSubmissionDTO>> gradeSubmission(
            @PathVariable UUID assignmentUuid,
            @PathVariable UUID submissionUuid,
            @RequestParam BigDecimal score,
            @RequestParam BigDecimal maxScore,
            @RequestParam(required = false) String comments) {
        AssignmentSubmissionDTO gradedSubmission = assignmentSubmissionService.gradeSubmission(
                submissionUuid, score, maxScore, comments);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(gradedSubmission, "Submission graded successfully"));
    }

    @Operation(
            summary = "Return submission for revision",
            description = "Returns a submission to student with feedback for revision."
    )
    @PostMapping("/{assignmentUuid}/submissions/{submissionUuid}/return")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<AssignmentSubmissionDTO>> returnSubmission(
            @PathVariable UUID assignmentUuid,
            @PathVariable UUID submissionUuid,
            @RequestParam String feedback) {
        AssignmentSubmissionDTO returnedSubmission = assignmentSubmissionService.returnForRevision(
                submissionUuid, feedback);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(returnedSubmission, "Submission returned for revision"));
    }

    // ===== ASSIGNMENT ANALYTICS =====

    @Operation(
            summary = "Get submission analytics",
            description = "Returns analytics data for assignment submissions including category distribution."
    )
    @GetMapping("/{assignmentUuid}/analytics")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Map<String, Long>>> getSubmissionAnalytics(
            @PathVariable UUID assignmentUuid) {
        Map<String, Long> analytics = assignmentSubmissionService.getSubmissionCategoryDistribution(assignmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(analytics, "Submission analytics retrieved successfully"));
    }

    @Operation(
            summary = "Get average submission score",
            description = "Returns the average score for all graded submissions of an assignment."
    )
    @GetMapping("/{assignmentUuid}/average-score")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Double>> getAverageScore(
            @PathVariable UUID assignmentUuid) {
        Double averageScore = assignmentSubmissionService.getAverageSubmissionScore(assignmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(averageScore, "Average submission score retrieved successfully"));
    }

    @Operation(
            summary = "Get high performance submissions",
            description = "Returns submissions with scores above 85%."
    )
    @GetMapping("/{assignmentUuid}/high-performance")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<AssignmentSubmissionDTO>>> getHighPerformanceSubmissions(
            @PathVariable UUID assignmentUuid) {
        List<AssignmentSubmissionDTO> highPerformers = assignmentSubmissionService.getHighPerformanceSubmissions(assignmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(highPerformers, "High performance submissions retrieved successfully"));
    }

    // ===== INSTRUCTOR WORKFLOWS =====

    @Operation(
            summary = "Get pending grading",
            description = "Retrieves all submissions pending grading for a specific instructor."
    )
    @GetMapping("/instructor/{instructorUuid}/pending-grading")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<AssignmentSubmissionDTO>>> getPendingGrading(
            @PathVariable UUID instructorUuid) {
        List<AssignmentSubmissionDTO> pendingSubmissions = assignmentSubmissionService.getPendingGrading(instructorUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(pendingSubmissions, "Pending submissions retrieved successfully"));
    }

    // ===== SEARCH ENDPOINTS =====

    @Operation(
            summary = "Search assignments",
            description = """
                    Advanced assignment search with flexible criteria and operators.
                    
                    **Common Assignment Search Examples:**
                    - `title_like=essay` - Assignments with "essay" in title
                    - `lessonUuid=uuid` - Assignments for specific lesson
                    - `is_published=true` - Only published assignments
                    - `dueDate_gte=2024-12-01T00:00:00` - Assignments due from Dec 1, 2024
                    - `maxPoints_gte=50` - Assignments worth 50+ points
                    """
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<AssignmentDTO>>> searchAssignments(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<AssignmentDTO> assignments = assignmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(assignments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Assignment search completed successfully"));
    }

    @Operation(
            summary = "Search assignment submissions",
            description = """
                    Search submissions across all assignments.
                    
                    **Common Submission Search Examples:**
                    - `assignmentUuid=uuid` - All submissions for specific assignment
                    - `enrollmentUuid=uuid` - All submissions by specific student
                    - `status=GRADED` - Only graded submissions
                    - `percentage_gte=90` - Submissions with 90%+ score
                    - `submittedAt_gte=2024-01-01T00:00:00` - Submissions from 2024
                    - `gradedByUuid=uuid` - Submissions graded by specific instructor
                    """
    )
    @GetMapping("/submissions/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<AssignmentSubmissionDTO>>> searchSubmissions(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<AssignmentSubmissionDTO> submissions = assignmentSubmissionService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(submissions, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Submission search completed successfully"));
    }
}
