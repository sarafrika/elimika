package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.internal.recommend.CourseFeatureRefresher;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Runs the nightly course feature refresh ({@code course_learning_stats}, {@code course_co_enrolments}) on
 * demand. Registered only under the {@code local} profile, where the seed script needs the tables filled
 * straight after seeding instead of at 01:00 UTC; no deployed environment runs that profile.
 */
@Profile("local")
@RestController
@RequiredArgsConstructor
@Tag(name = "Course Recommendations", description = "Similar courses and recommender evaluation")
public class LocalCourseFeatureRefreshController {

    private final CourseFeatureRefresher refresher;

    @Operation(operationId = "refreshCourseFeaturesLocal",
            summary = "Refresh course feature tables now (local profile only)",
            description = "Platform admin. Rewrites course_learning_stats and course_co_enrolments, exactly as the "
                    + "nightly CourseFeatureRefreshJob does. Exists only when the `local` profile is active.")
    @PreAuthorize("@domainSecurityService.isPlatformAdmin()")
    @PostMapping("/api/v1/admin/recommendations/features/refresh")
    public ResponseEntity<ApiResponse<RefreshOutcome>> refresh() {
        CourseFeatureRefresher.Outcome outcome = refresher.refresh(Instant.now());
        return ResponseEntity.ok(ApiResponse.success(
                new RefreshOutcome(outcome.ran(), outcome.statsRows(), outcome.coEnrolmentRows()),
                "Course features refreshed"));
    }

    public record RefreshOutcome(
            @JsonProperty("ran") boolean ran,
            @JsonProperty("stats_rows") int statsRows,
            @JsonProperty("co_enrolment_rows") int coEnrolmentRows) {
    }
}
