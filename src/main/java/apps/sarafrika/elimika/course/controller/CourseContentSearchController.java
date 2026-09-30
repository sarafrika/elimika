package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.internal.search.CourseContentSearchHitDTO;
import apps.sarafrika.elimika.course.internal.search.CourseContentSearchService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * In-course search: the lessons, content items, quizzes and assignments of one course, served from
 * the {@code course_content} index.
 */
@RestController
@RequestMapping(CourseContentSearchController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Content Search", description = "Full-text search inside one course")
public class CourseContentSearchController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    /**
     * Course read access: a platform admin (capped by the dashboard), the course's staff (owner, or a
     * member of an organisation approved to train it), an enrolled learner, or anyone who manages it.
     */
    private static final String COURSE_READ =
            "@learnerContentAccess.isPlatformAdmin() or @courseSecurityService.canReadCourseAsLearner(#courseUuid) "
                    + "or @courseSecurityService.canManageCourseGradebook(#courseUuid)";

    private final CourseContentSearchService courseContentSearchService;

    @Operation(
            operationId = "searchCourseContent",
            summary = "Search inside a course",
            description = """
                    Full-text, typo-tolerant search over one course's lessons, lesson content, quizzes and
                    assignments. Quiz questions and answers, rubrics, submissions and file URLs are never
                    searched.

                    - Staff who manage the course (its author, instructors and organisations approved to
                      train it) see every item, drafts included.
                    - Enrolled learners see published, course-level items only; class-specific quizzes and
                      assignments are not searchable yet.

                    Hits come back in relevance order with the item `type` (`lesson`, `content`, `quiz`,
                    `assignment`), its `uuid`, its lesson (`lesson_uuid`, `lesson_number`,
                    `lesson_title`), its `title` and a `highlight` excerpt with `<em>` markers.

                    **403** when the caller may not read the course. **400** when `q` is missing, a type is
                    unknown or the page is out of range. **503** `Search is unavailable` when search, or
                    the `course_content` index's reads, are off or the engine fails.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching items")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing q, unknown type or bad page")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The caller may not read this course")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Search is unavailable")
    @PreAuthorize(COURSE_READ)
    @GetMapping("/{courseUuid}/content/search")
    public ResponseEntity<ApiResponse<PagedDTO<CourseContentSearchHitDTO>>> searchCourseContent(
            @PathVariable UUID courseUuid,
            @Parameter(description = "Query text", required = true)
            @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Comma-separated item types: lesson, content, quiz, assignment; all when omitted")
            @RequestParam(value = "types", required = false) String types,
            @Parameter(description = "0-based page number (default 0)")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size, 1-100 (default 20)")
            @RequestParam(value = "size", required = false) Integer size) {
        Page<CourseContentSearchHitDTO> hits = courseContentSearchService.search(courseUuid, q, types, page, size);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(hits, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Course content search results retrieved successfully"));
    }
}
