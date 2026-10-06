package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CourseEvaluationPlanDTO;
import apps.sarafrika.elimika.course.dto.CourseEvaluationPlanUpdateRequest;
import apps.sarafrika.elimika.course.service.CourseEvaluationPlanService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseUuid}/evaluation-plan")
@RequiredArgsConstructor
@Tag(name = "Course Evaluation Plan", description = "Lesson by lesson grading: which component grades which lesson, and with which rubric")
public class CourseEvaluationPlanController {

    private final CourseEvaluationPlanService evaluationPlanService;

    @Operation(operationId = "getCourseEvaluationPlan", summary = "Get the lesson x component evaluation plan")
    @GetMapping
    @PreAuthorize("@courseSecurityService.canManageCourseGradebook(#courseUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<CourseEvaluationPlanDTO>> getPlan(@PathVariable UUID courseUuid) {
        return ResponseEntity.ok(ApiResponse.success(evaluationPlanService.getPlan(courseUuid),
                "Evaluation plan retrieved successfully"));
    }

    @Operation(operationId = "updateCourseEvaluationPlan", summary = "Turn evaluation plan cells on or off",
            description = "Each cell grades one lesson for one per-lesson component, optionally with a rubric, quiz or "
                    + "assignment from that lesson. A disabled cell is the plan's \"None\".")
    @PutMapping
    @PreAuthorize("@courseSecurityService.isCourseOwner(#courseUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<CourseEvaluationPlanDTO>> updatePlan(
            @PathVariable UUID courseUuid, @Valid @RequestBody CourseEvaluationPlanUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(evaluationPlanService.updatePlan(courseUuid, request.cells()),
                "Evaluation plan updated successfully"));
    }
}
