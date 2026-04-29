package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.LessonPracticeActivityDTO;
import apps.sarafrika.elimika.course.service.LessonPracticeActivityService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(LessonPracticeActivityController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lesson Practice Activity Management", description = "Manage reusable practice activity templates for lessons.")
public class LessonPracticeActivityController {

    public static final String API_ROOT_PATH = "/api/v1/courses/{courseUuid}/lessons/{lessonUuid}/practice-activities";

    private final LessonPracticeActivityService practiceActivityService;

    @Operation(summary = "Create a lesson practice activity")
    @PostMapping
    public ResponseEntity<ApiResponse<LessonPracticeActivityDTO>> createPracticeActivity(
            @Parameter(description = "Course UUID", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Lesson UUID", required = true)
            @PathVariable UUID lessonUuid,
            @Valid @RequestBody LessonPracticeActivityDTO request) {
        log.debug("Creating practice activity for course {} lesson {}", courseUuid, lessonUuid);
        LessonPracticeActivityDTO result = practiceActivityService.createPracticeActivity(courseUuid, lessonUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Practice activity created successfully"));
    }

    @Operation(summary = "List lesson practice activities")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<LessonPracticeActivityDTO>>> getPracticeActivities(
            @Parameter(description = "Course UUID", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Lesson UUID", required = true)
            @PathVariable UUID lessonUuid,
            Pageable pageable) {
        log.debug("Fetching practice activities for course {} lesson {}", courseUuid, lessonUuid);
        Page<LessonPracticeActivityDTO> result =
                practiceActivityService.getPracticeActivitiesByLesson(courseUuid, lessonUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(result, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Practice activities retrieved successfully"
        ));
    }

    @Operation(summary = "Get a lesson practice activity")
    @GetMapping("/{activityUuid}")
    public ResponseEntity<ApiResponse<LessonPracticeActivityDTO>> getPracticeActivity(
            @Parameter(description = "Course UUID", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Lesson UUID", required = true)
            @PathVariable UUID lessonUuid,
            @Parameter(description = "Practice activity UUID", required = true)
            @PathVariable UUID activityUuid) {
        log.debug("Fetching practice activity {} for course {} lesson {}", activityUuid, courseUuid, lessonUuid);
        LessonPracticeActivityDTO result = practiceActivityService.getPracticeActivity(courseUuid, lessonUuid, activityUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Practice activity retrieved successfully"));
    }

    @Operation(summary = "Update a lesson practice activity")
    @PutMapping("/{activityUuid}")
    public ResponseEntity<ApiResponse<LessonPracticeActivityDTO>> updatePracticeActivity(
            @Parameter(description = "Course UUID", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Lesson UUID", required = true)
            @PathVariable UUID lessonUuid,
            @Parameter(description = "Practice activity UUID", required = true)
            @PathVariable UUID activityUuid,
            @Valid @RequestBody LessonPracticeActivityDTO request) {
        log.debug("Updating practice activity {} for course {} lesson {}", activityUuid, courseUuid, lessonUuid);
        LessonPracticeActivityDTO result = practiceActivityService.updatePracticeActivity(courseUuid, lessonUuid, activityUuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Practice activity updated successfully"));
    }

    @Operation(summary = "Delete a lesson practice activity")
    @DeleteMapping("/{activityUuid}")
    public ResponseEntity<Void> deletePracticeActivity(
            @Parameter(description = "Course UUID", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Lesson UUID", required = true)
            @PathVariable UUID lessonUuid,
            @Parameter(description = "Practice activity UUID", required = true)
            @PathVariable UUID activityUuid) {
        log.debug("Deleting practice activity {} for course {} lesson {}", activityUuid, courseUuid, lessonUuid);
        practiceActivityService.deletePracticeActivity(courseUuid, lessonUuid, activityUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reorder lesson practice activities")
    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<String>> reorderPracticeActivities(
            @Parameter(description = "Course UUID", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Lesson UUID", required = true)
            @PathVariable UUID lessonUuid,
            @RequestBody List<UUID> activityUuids) {
        log.debug("Reordering practice activities for course {} lesson {}", courseUuid, lessonUuid);
        practiceActivityService.reorderPracticeActivities(courseUuid, lessonUuid, activityUuids);
        return ResponseEntity.ok(ApiResponse.success("Practice activities reordered successfully", "Practice activity order updated"));
    }
}
