package apps.sarafrika.elimika.course.controller.admin;

import apps.sarafrika.elimika.course.dto.ContentModerationDecisionRequest;
import apps.sarafrika.elimika.course.dto.ContentModerationHistoryDTO;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.CourseEditDiffDTO;
import apps.sarafrika.elimika.course.dto.CoursePendingEditDTO;
import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import apps.sarafrika.elimika.course.service.ContentModerationHistoryService;
import apps.sarafrika.elimika.course.service.CoursePendingEditService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.TrainingProgramService;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Content Moderation", description = "Approve or revoke course and training program availability")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class ContentApprovalAdminController {

    private final CourseService courseService;
    private final TrainingProgramService trainingProgramService;
    private final ContentModerationHistoryService contentModerationHistoryService;
    private final CoursePendingEditService coursePendingEditService;

    @GetMapping("/courses/pending")
    @Operation(
            summary = "List courses pending first approval",
            description = """
                    Courses that have never been approved and are waiting on an initial decision.

                    This does **not** include already-published courses with a pending edit: those
                    keep `admin_approved = true` while their edit is reviewed, so they never match
                    this query. Use `GET /api/v1/admin/courses/pending-edits` for those.
                    """
    )
    public ResponseEntity<ApiResponse<PagedDTO<CourseDTO>>> listPendingCourses(Pageable pageable) {
        Page<CourseDTO> pending = courseService.search(
                Map.of("admin_approved", "false", "status_in", "IN_REVIEW,PUBLISHED"), pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(pending, baseUrl), "Pending courses retrieved successfully"));
    }

    @GetMapping("/courses/pending-edits")
    @Operation(
            summary = "List edits to published courses awaiting review",
            description = """
                    Edits submitted against already-published courses, newest first.

                    Each of these courses is still live and serving its last-approved content —
                    the proposed change is held on a draft and is invisible to learners until
                    approved. Approving promotes the draft onto the live course; rejecting
                    discards it and leaves the live course exactly as it was.
                    """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending edits retrieved successfully")
            }
    )
    public ResponseEntity<ApiResponse<PagedDTO<CoursePendingEditDTO>>> listPendingCourseEdits(Pageable pageable) {
        Page<CoursePendingEditDTO> pending = coursePendingEditService.getPendingQueue(pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(pending, baseUrl),
                "Pending course edits retrieved successfully"));
    }

    @GetMapping("/courses/{uuid}/pending-edit/diff")
    @Operation(
            summary = "Show what a pending edit would change",
            description = """
                    The difference between the live course and the edit awaiting review: which
                    course fields change and how many lessons the edit adds, removes or modifies.
                    """,
            // Response schemas are inferred from the return type so the generated clients see
            // the real ApiResponse envelope rather than the bare payload.
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Diff retrieved"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No edit awaiting review for this course")
            }
    )
    public ResponseEntity<ApiResponse<CourseEditDiffDTO>> getCourseEditDiff(
            @Parameter(description = "UUID of the live course") @PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(coursePendingEditService.diff(uuid),
                "Course edit diff retrieved successfully"));
    }

    @PostMapping("/courses/{uuid}/moderate")
    @Operation(
            summary = "Moderate a course or its pending edit",
            description = """
                    Applies a moderation decision to a course.

                    **When the course has an edit awaiting review**, the decision applies to that
                    edit: `approved` promotes the draft onto the live course and records a new
                    version; `rejected` discards the draft and leaves the live course untouched,
                    including its approval — the published content was never at fault.

                    **Otherwise** the decision applies to the course's own approval state, as
                    before: `approved` makes it available, `rejected`/`revoked` withdraw it.

                    Every decision is recorded in the course's moderation history with its reason.
                    """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Decision applied successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unsupported action"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    public ResponseEntity<ApiResponse<CourseDTO>> moderateCourse(
            @Parameter(description = "UUID of the course") @PathVariable UUID uuid,
            @Valid @RequestBody ContentModerationDecisionRequest request) {

        boolean hasPendingEdit = coursePendingEditService.findPending(uuid).isPresent();
        ModerationAction action = request.action();
        String reason = request.reason();

        if (hasPendingEdit && action != ModerationAction.REVOKED) {
            // The decision is about the proposed change, not the live course.
            if (action == ModerationAction.APPROVED) {
                coursePendingEditService.approve(uuid, reason);
            } else {
                coursePendingEditService.reject(uuid, reason);
            }
            contentModerationHistoryService.record(ModerationContentType.COURSE, uuid, action, reason);
            CourseDTO course = courseService.getCourseByUuid(uuid);
            String message = action == ModerationAction.APPROVED
                    ? "Course edit approved and published successfully"
                    : "Course edit rejected successfully; the live course is unchanged";
            return ResponseEntity.ok(ApiResponse.success(course, message));
        }

        CourseDTO course = switch (action) {
            case APPROVED -> courseService.approveCourse(uuid, reason);
            case REJECTED -> courseService.unapproveCourse(uuid, reason, ModerationAction.REJECTED);
            case REVOKED -> courseService.unapproveCourse(uuid, reason, ModerationAction.REVOKED);
        };

        String message = switch (action) {
            case APPROVED -> "Course approved successfully";
            case REJECTED -> "Course rejected successfully";
            case REVOKED -> "Course approval revoked successfully";
        };
        return ResponseEntity.ok(ApiResponse.success(course, message));
    }

    @GetMapping("/courses/{uuid}/approval-status")
    @Operation(summary = "Get course approval status")
    public ResponseEntity<ApiResponse<Boolean>> getCourseApprovalStatus(@PathVariable UUID uuid) {
        boolean approved = courseService.isCourseApproved(uuid);
        String message = approved ? "Course is approved" : "Course is not approved";
        return ResponseEntity.ok(ApiResponse.success(approved, message));
    }

    @GetMapping("/courses/{uuid}/moderation-history")
    @Operation(operationId = "getCourseModerationHistory", summary = "Get course moderation history")
    public ResponseEntity<ApiResponse<PagedDTO<ContentModerationHistoryDTO>>> getCourseModerationHistory(
            @PathVariable UUID uuid, Pageable pageable) {
        Page<ContentModerationHistoryDTO> history =
                contentModerationHistoryService.getHistory(ModerationContentType.COURSE, uuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(history, baseUrl),
                "Course moderation history retrieved successfully"));
    }

    @GetMapping("/programs/pending")
    @Operation(summary = "List training programs pending approval")
    public ResponseEntity<ApiResponse<PagedDTO<TrainingProgramDTO>>> listPendingPrograms(Pageable pageable) {
        Page<TrainingProgramDTO> pending = trainingProgramService.search(
                Map.of("admin_approved", "false", "status_in", "IN_REVIEW,PUBLISHED"), pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(pending, baseUrl), "Pending training programs retrieved successfully"));
    }

    @PostMapping("/programs/{uuid}/moderate")
    @Operation(summary = "Moderate training program approval")
    public ResponseEntity<ApiResponse<TrainingProgramDTO>> moderateProgram(
            @Parameter(description = "UUID of the training program") @PathVariable UUID uuid,
            @Valid @RequestBody ContentModerationDecisionRequest request) {

        ModerationAction action = request.action();
        String reason = request.reason();

        TrainingProgramDTO program = switch (action) {
            case APPROVED -> trainingProgramService.approveProgram(uuid, reason);
            case REJECTED -> trainingProgramService.unapproveProgram(uuid, reason, ModerationAction.REJECTED);
            case REVOKED -> trainingProgramService.unapproveProgram(uuid, reason, ModerationAction.REVOKED);
        };

        String message = switch (action) {
            case APPROVED -> "Training program approved successfully";
            case REJECTED -> "Training program rejected successfully";
            case REVOKED -> "Training program approval revoked successfully";
        };
        return ResponseEntity.ok(ApiResponse.success(program, message));
    }

    @GetMapping("/programs/{uuid}/approval-status")
    @Operation(summary = "Get training program approval status")
    public ResponseEntity<ApiResponse<Boolean>> getProgramApprovalStatus(@PathVariable UUID uuid) {
        boolean approved = trainingProgramService.isProgramApproved(uuid);
        String message = approved ? "Training program is approved" : "Training program is not approved";
        return ResponseEntity.ok(ApiResponse.success(approved, message));
    }

    @GetMapping("/programs/{uuid}/moderation-history")
    @Operation(operationId = "getProgramModerationHistory", summary = "Get training program moderation history")
    public ResponseEntity<ApiResponse<PagedDTO<ContentModerationHistoryDTO>>> getProgramModerationHistory(
            @PathVariable UUID uuid, Pageable pageable) {
        Page<ContentModerationHistoryDTO> history =
                contentModerationHistoryService.getHistory(ModerationContentType.TRAINING_PROGRAM, uuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(history, baseUrl),
                "Training program moderation history retrieved successfully"));
    }

}
