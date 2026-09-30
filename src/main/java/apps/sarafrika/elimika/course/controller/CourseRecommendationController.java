package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO;
import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;
import apps.sarafrika.elimika.course.service.CourseRecommendationService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Non-personal "similar courses" and the offline evaluation of the recommender. The personal list lives
 * on {@code GET /api/v1/courses/recommendations}.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Course Recommendations", description = "Similar courses and recommender evaluation")
public class CourseRecommendationController {

    private final CourseRecommendationService courseRecommendationService;

    @Operation(operationId = "getSimilarCourses", summary = "Courses similar to a public course",
            description = "Anyone, signed in or not. Not personal: co-enrolment neighbours (pairs shared by at least 5 "
                    + "learners, 10 when a minor is involved), the same categories, and \"more like this\" on the "
                    + "course text. Only public courses; 404 when the course itself is not public. Same item shape "
                    + "as the recommendations endpoint, with surface `similar`.")
    @GetMapping("/api/v1/courses/{uuid}/similar")
    public ResponseEntity<ApiResponse<List<RecommendedCourseDTO>>> getSimilarCourses(
            @PathVariable UUID uuid,
            @Parameter(description = "Maximum number of courses (default 6, max 50)")
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(courseRecommendationService.findSimilar(uuid, limit),
                "Similar courses retrieved successfully"));
    }

    @Operation(operationId = "evaluateCourseRecommendations", summary = "Offline evaluation of course recommendations",
            description = "Platform admin. Leave-last-out over course enrolments: recall@6, nDCG@6 and coverage for "
                    + "rules-v2, a popularity baseline and the legacy newest-first list. A regression gate, not a "
                    + "tuning target. Aggregates only.")
    @PreAuthorize("@domainSecurityService.isPlatformAdmin()")
    @GetMapping("/api/v1/admin/recommendations/evaluation")
    public ResponseEntity<ApiResponse<RecommendationEvaluationDTO>> evaluate() {
        return ResponseEntity.ok(ApiResponse.success(courseRecommendationService.evaluate(),
                "Recommendation evaluation completed"));
    }
}
