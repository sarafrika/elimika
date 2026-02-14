package apps.sarafrika.elimika.course.controller.admin;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.TrainingProgramService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Content Moderation", description = "Approve or revoke course and training program availability")
@PreAuthorize("@domainSecurityService.isOrganizationAdmin()")
public class ContentApprovalAdminController {

    private final CourseService courseService;
    private final TrainingProgramService trainingProgramService;

    @GetMapping("/courses/pending")
    @Operation(summary = "List courses pending approval")
    public ResponseEntity<ApiResponse<PagedDTO<CourseDTO>>> listPendingCourses(Pageable pageable) {
        Page<CourseDTO> pending = courseService.search(Map.of("admin_approved", "false"), pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(pending, baseUrl), "Pending courses retrieved successfully"));
    }

    @PostMapping("/courses/{uuid}/moderate")
    @Operation(summary = "Moderate course approval")
    public ResponseEntity<ApiResponse<CourseDTO>> moderateCourse(@PathVariable UUID uuid,
                                                                 @RequestParam("action") String action,
                                                                 @RequestParam(required = false) String reason) {
        String normalizedAction = normalizeAction(action);
        CourseDTO course = switch (normalizedAction) {
            case "approve" -> courseService.approveCourse(uuid, reason);
            case "reject", "revoke" -> courseService.unapproveCourse(uuid, reason);
            default -> throw unsupportedAction(action);
        };

        String message = switch (normalizedAction) {
            case "approve" -> "Course approved successfully";
            case "reject" -> "Course rejected successfully";
            default -> "Course approval revoked successfully";
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

    @GetMapping("/programs/pending")
    @Operation(summary = "List training programs pending approval")
    public ResponseEntity<ApiResponse<PagedDTO<TrainingProgramDTO>>> listPendingPrograms(Pageable pageable) {
        Page<TrainingProgramDTO> pending = trainingProgramService.search(Map.of("admin_approved", "false"), pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(pending, baseUrl), "Pending training programs retrieved successfully"));
    }

    @PostMapping("/programs/{uuid}/moderate")
    @Operation(summary = "Moderate training program approval")
    public ResponseEntity<ApiResponse<TrainingProgramDTO>> moderateProgram(@PathVariable UUID uuid,
                                                                           @RequestParam("action") String action,
                                                                           @RequestParam(required = false) String reason) {
        String normalizedAction = normalizeAction(action);
        TrainingProgramDTO program = switch (normalizedAction) {
            case "approve" -> trainingProgramService.approveProgram(uuid, reason);
            case "reject", "revoke" -> trainingProgramService.unapproveProgram(uuid, reason);
            default -> throw unsupportedAction(action);
        };

        String message = switch (normalizedAction) {
            case "approve" -> "Training program approved successfully";
            case "reject" -> "Training program rejected successfully";
            default -> "Training program approval revoked successfully";
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

    private String normalizeAction(String action) {
        if (action == null) {
            return "";
        }
        return action.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException unsupportedAction(String action) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported moderation action: " + action + ". Allowed values: approve, reject, revoke"
        );
    }
}
