package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CourseStatsDTO;
import apps.sarafrika.elimika.course.service.CourseStatsService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A course's performance, told to each caller at the depth they are entitled to.
 * <p>
 * The route itself only asks for a signed-in caller. Entitlement is not one decision but three, so
 * it is settled block by block while the response is assembled, and a block the caller may not read
 * is left out of the JSON entirely rather than nulled or zeroed.
 */
@RestController
@RequestMapping(CourseStatsController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Statistics", description = "Course performance, scoped to what the caller is entitled to see")
public class CourseStatsController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CourseStatsService courseStatsService;

    @Operation(
            summary = "Get course statistics scoped to the caller",
            description = """
                    Returns up to three blocks, and omits any the caller has not earned.

                    - `public` — always present. Learners trained, classes running, mean seat fill,
                      completion rate, rating and how many trainers are approved to deliver the
                      course. Seat fill is a percentage rounded to the nearest 5; the filled and
                      total seat counts behind it are never published, because printed beside a
                      course's price they make gross revenue a multiplication.
                    - `scoped` — only for an instructor, or a member of an organisation, holding an
                      **approved** application to train this course. Covers their own classes alone.
                      A pending application grants nothing: anybody may lodge one.
                    - `owner` — only for the course creator and platform admins. Commercial totals.

                    An absent block means "not yours to see". It is never a zero to be rendered.
                    """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Statistics retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404", description = "Course not found")
            }
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{courseUuid}/stats")
    public ResponseEntity<ApiResponse<CourseStatsDTO>> getCourseStats(@PathVariable UUID courseUuid) {
        CourseStatsDTO stats = courseStatsService.getCourseStats(courseUuid);
        return ResponseEntity.ok(ApiResponse.success(stats, "Course statistics retrieved successfully"));
    }
}
