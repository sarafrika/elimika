package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CourseSkillDTO;
import apps.sarafrika.elimika.course.dto.CourseSkillsUpdateRequest;
import apps.sarafrika.elimika.course.service.CourseSkillService;
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

/** A course's skill tags. Optional; they never gate publishing. */
@RestController
@RequestMapping(CourseSkillController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Skills", description = "Skills a course teaches, tagged by its creator from the skills taxonomy")
public class CourseSkillController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CourseSkillService courseSkillService;

    @Operation(operationId = "getCourseSkills", summary = "Get a course's skill tags",
            description = "Readable by anyone who can read the course (404 otherwise). Heaviest first.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{uuid}/skills")
    public ResponseEntity<ApiResponse<List<CourseSkillDTO>>> getCourseSkills(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(courseSkillService.getCourseSkills(uuid),
                "Course skills retrieved successfully"));
    }

    @Operation(operationId = "replaceCourseSkills", summary = "Replace a course's skill tags",
            description = "Course owner only. The body is the complete list; [] clears it. Skills must come from "
                    + "GET /api/v1/skills: an unknown skill, a duplicate, or a newly added retired skill is a 400. "
                    + "Tags go on the live course, never on a pending shadow draft (400). "
                    + "Marketplace jobs of the course without tags of their own inherit these.")
    @PreAuthorize("@courseSecurityService.isCourseOwner(#uuid)")
    @PutMapping("/{uuid}/skills")
    public ResponseEntity<ApiResponse<List<CourseSkillDTO>>> replaceCourseSkills(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseSkillsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseSkillService.replaceCourseSkills(uuid, request),
                "Course skills updated successfully"));
    }
}
