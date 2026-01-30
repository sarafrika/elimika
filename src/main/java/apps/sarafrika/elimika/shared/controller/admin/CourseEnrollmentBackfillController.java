package apps.sarafrika.elimika.shared.controller.admin;

import apps.sarafrika.elimika.course.internal.CourseEnrollmentBackfillService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/course-enrollments")
@RequiredArgsConstructor
@Tag(name = "Course Enrollment Admin", description = "Operational endpoints for course enrollment maintenance")
public class CourseEnrollmentBackfillController {

    private final CourseEnrollmentBackfillService courseEnrollmentBackfillService;

    @Operation(summary = "Backfill course enrollments", description = "Creates or syncs course enrollments from class and program enrollments")
    @PostMapping("/backfill")
    public ResponseEntity<ApiResponse<Integer>> backfillCourseEnrollments() {
        int processed = courseEnrollmentBackfillService.backfillCourseEnrollments();
        return ResponseEntity.ok(ApiResponse.success(processed, "Course enrollment backfill completed"));
    }
}
