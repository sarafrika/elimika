package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CourseOpenClasses;
import apps.sarafrika.elimika.course.service.CourseOpenClassService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
 * The classes a visitor can join on a public course page, readable without signing in.
 */
@RestController
@RequestMapping(CourseOpenClassesController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Open Classes", description = "Joinable classes of a public course, for the signed-out course page")
public class CourseOpenClassesController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CourseOpenClassService courseOpenClassService;

    @Operation(
            operationId = "getCourseOpenClasses",
            summary = "List the classes a visitor can still join on a public course",
            description = """
                    Readable without a token. Answers only for a publicly visible course (root, published,
                    active and admin-approved); any other course is a 404 for every caller.

                    Lists the course's classes that are active, `PUBLIC`, and whose registration window
                    and teaching period have not ended (a missing end date counts as open), cheapest
                    first, then soonest start.

                    `price_from` is the lowest class fee among them. That class fee (`fee`, the class
                    sale price) is what a learner pays; the course's own `price` is not. `price_from`
                    is null when there are no classes.

                    Never carries coordinates, meeting links, instructor or organisation identifiers,
                    instructor pay or revenue terms. `place_name` and `area` come from the class's
                    location label only.
                    """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Open classes retrieved"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404", description = "Course not found or not publicly visible")
            }
    )
    @SecurityRequirements
    @PreAuthorize("permitAll()")
    @GetMapping("/{courseUuid}/open-classes")
    public ResponseEntity<ApiResponse<CourseOpenClasses>> getCourseOpenClasses(@PathVariable UUID courseUuid) {
        return ResponseEntity.ok(ApiResponse.success(courseOpenClassService.getOpenClasses(courseUuid),
                "Open classes retrieved successfully"));
    }
}
