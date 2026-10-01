package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CoursePrerequisiteDTO;
import apps.sarafrika.elimika.course.dto.CoursePrerequisitesRequest;
import apps.sarafrika.elimika.course.service.CoursePrerequisiteService;
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

import java.util.List;
import java.util.UUID;

/**
 * The prior courses a course requires or recommends, edited as one set in the course editor.
 */
@RestController
@RequestMapping(CoursePrerequisiteController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Prerequisites", description = "Required and recommended prior courses of a course")
public class CoursePrerequisiteController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CoursePrerequisiteService coursePrerequisiteService;

    @Operation(
            operationId = "getCoursePrerequisites",
            summary = "List a course's prerequisites",
            description = """
                    The prior courses this course requires (`is_mandatory: true`) or recommends. Readable by anyone
                    who can read the course; a course the caller may not read answers 404. Anonymous visitors
                    may read the prerequisites of a public course (published, active, admin-approved).

                    On a live course with a pending edit this returns the live set. The author reads the proposed
                    set from the draft course (`draft_course_uuid` on the pending edit).
                    """)
    @GetMapping("/{uuid}/prerequisites")
    public ResponseEntity<ApiResponse<List<CoursePrerequisiteDTO>>> getPrerequisites(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(
                coursePrerequisiteService.getPrerequisites(uuid), "Course prerequisites retrieved successfully"));
    }

    @Operation(
            operationId = "replaceCoursePrerequisites",
            summary = "Replace a course's prerequisites",
            description = """
                    Replaces the whole prerequisite set; an empty list clears it. Course owner only.

                    Each prior course must be a published course or one of the author's own courses. A course
                    cannot require itself, list a course twice, or close a cycle (A requires B requires A): all
                    answer 400.

                    On a live, approved course the change lands on the course's draft and goes through review like
                    any other edit; the response is then the draft's set, and `course_uuid` is the draft.
                    """)
    @PreAuthorize("@courseSecurityService.isCourseOwner(#uuid)")
    @PutMapping("/{uuid}/prerequisites")
    public ResponseEntity<ApiResponse<List<CoursePrerequisiteDTO>>> replacePrerequisites(
            @PathVariable UUID uuid,
            @Valid @RequestBody CoursePrerequisitesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                coursePrerequisiteService.replacePrerequisites(uuid, request), "Course prerequisites updated successfully"));
    }
}
