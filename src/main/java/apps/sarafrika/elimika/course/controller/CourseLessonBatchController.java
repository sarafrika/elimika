package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.service.CourseLessonBatchService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Reads the lessons of many courses in one request instead of one lessons call per course. */
@RestController
@RequestMapping(CourseLessonBatchController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Complete course lifecycle management including content, assessments, media, analytics, and category management")
public class CourseLessonBatchController {

    public static final String API_ROOT_PATH = "/api/v1/courses/lessons";

    private final CourseLessonBatchService courseLessonBatchService;

    @Operation(summary = "List the lessons of several courses",
            description = "course_uuids is comma-separated or repeated, 1 to " + CourseLessonBatchService.MAX_BATCH_SIZE
                    + " entries. Each course is admitted by the same rule as GET /courses/{courseUuid}/lessons, and "
                    + "learners see only published, active lessons. Courses the caller may not read are omitted. "
                    + "Ordered by course then lesson number; group by course_uuid.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course lessons retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No course uuids, or more than the maximum, requested")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LessonDTO>>> getLessonsForCourses(
            @Parameter(description = "Course UUIDs, comma-separated", required = true)
            @RequestParam(value = "course_uuids", required = false) List<UUID> courseUuids) {
        // Optional at binding only so a missing list reaches the service's 400 instead of the generic 500.
        List<LessonDTO> result = courseLessonBatchService.getLessons(courseUuids);
        return ResponseEntity.ok(ApiResponse.success(result, "Course lessons retrieved successfully"));
    }
}
