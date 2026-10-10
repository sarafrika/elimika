package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewDTO;
import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewItemDTO;
import apps.sarafrika.elimika.classes.service.StudentCourseOverviewService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * A learner's course list in one round trip. Lives in the classes module because it composes class
 * definitions and assessment schedules with timetabling, course and instructor data.
 */
@RestController
@RequestMapping("/api/v1/enrollment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment API", description = "Student enrollment and attendance management")
public class StudentCourseOverviewController {

    private final StudentCourseOverviewService studentCourseOverviewService;

    @Operation(
            summary = "Get a student's course overview",
            description = "Enrolled classes with their course, instructor, current or next session, course "
                    + "progress and counts of released assignments and quizzes not yet submitted, in one "
                    + "response. Readable by the student, a guardian whose share covers academics, and "
                    + "platform administrators. Page size is capped at 50."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course overview retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not the student, an academic guardian or a platform administrator")
    @GetMapping("/student/{studentUuid}/course-overview")
    @PreAuthorize("@studentCourseOverviewAccess.canRead(#studentUuid)")
    public ResponseEntity<ApiResponse<StudentCourseOverviewDTO>> getStudentCourseOverview(
            @Parameter(description = "UUID of the student")
            @PathVariable UUID studentUuid,
            Pageable pageable) {
        log.debug("REST request to get course overview for student: {}", studentUuid);

        Page<StudentCourseOverviewItemDTO> page = studentCourseOverviewService.getCourseOverview(studentUuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        StudentCourseOverviewDTO overview = new StudentCourseOverviewDTO(studentUuid, PagedDTO.from(page, baseUrl));
        return ResponseEntity.ok(ApiResponse.success(overview, "Student course overview retrieved successfully"));
    }
}
