package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.course.util.enums.LessonType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Lesson Controller
 * <p>
 * REST API controller for lesson management operations in the Sarafrika Elimika system.
 * Provides endpoints for creating, retrieving, updating, deleting, and searching lessons
 * within courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
@RestController
@RequestMapping("api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lessons API", description = "Lesson management and course content operations")
public class LessonController {

    private final LessonService lessonService;

    @Operation(
            summary = "Create a new lesson",
            description = "Creates a new lesson within a course. The lesson number must be unique " +
                    "within the specified course, and the associated course must exist."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Lesson created successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation errors"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Associated course not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Lesson number already exists for the course"
    )
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LessonDTO>> createLesson(
            @Parameter(description = "Lesson information to create", required = true)
            @Valid @RequestBody LessonDTO lessonDTO) {
        LessonDTO created = lessonService.createLesson(lessonDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Lesson created successfully"));
    }

    @Operation(
            summary = "Get a lesson by UUID",
            description = "Retrieves a specific lesson by its unique identifier (UUID)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Lesson not found"
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<LessonDTO>> getLessonByUuid(
            @Parameter(description = "Unique identifier of the lesson", required = true)
            @PathVariable UUID uuid) {
        return lessonService.getLessonByUuid(uuid)
                .map(lesson -> ResponseEntity.ok(ApiResponse.success(lesson, "Lesson retrieved successfully")))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get lessons by course",
            description = "Retrieves all lessons for a specific course, ordered by lesson number. " +
                    "Returns a complete list without pagination."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lessons retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @GetMapping("/course/{courseUuid}")
    public ResponseEntity<ApiResponse<List<LessonDTO>>> getLessonsByCourse(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid) {
        List<LessonDTO> lessons = lessonService.getLessonsByCourse(courseUuid);
        return ResponseEntity.ok(ApiResponse.success(lessons, "Lessons retrieved successfully"));
    }

    @Operation(
            summary = "Get lessons by course with pagination",
            description = "Retrieves lessons for a specific course with pagination support. " +
                    "Useful for large courses with many lessons."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lessons retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @GetMapping("/course/{courseUuid}/paged")
    public ResponseEntity<ApiResponse<PagedDTO<LessonDTO>>> getLessonsByCourseWithPagination(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LessonDTO> lessons = lessonService.getLessonsByCourse(courseUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(lessons, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Lessons retrieved successfully"));
    }

    @Operation(
            summary = "Get lessons by type",
            description = "Retrieves lessons filtered by their content type (TEXT, VIDEO, PDF, YOUTUBE_LINK) " +
                    "with pagination support."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lessons retrieved successfully"
    )
    @GetMapping("/type/{lessonType}")
    public ResponseEntity<ApiResponse<PagedDTO<LessonDTO>>> getLessonsByType(
            @Parameter(description = "Type of lesson content", required = true)
            @PathVariable LessonType lessonType,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LessonDTO> lessons = lessonService.getLessonsByType(lessonType, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(lessons, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Lessons retrieved successfully"));
    }

    @Operation(
            summary = "Update a lesson",
            description = "Updates an existing lesson with the provided information. " +
                    "Only non-null fields in the request body will be updated. " +
                    "Lesson number changes are validated for uniqueness within the course."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson updated successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation errors"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Lesson not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Lesson number conflict within the course"
    )
    @PutMapping(value = "/{uuid}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LessonDTO>> updateLesson(
            @Parameter(description = "Unique identifier of the lesson to update", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Updated lesson information", required = true)
            @Valid @RequestBody LessonDTO lessonDTO) {
        LessonDTO updated = lessonService.updateLesson(uuid, lessonDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Lesson updated successfully"));
    }

    @Operation(
            summary = "Update lesson number",
            description = "Updates the lesson number for a specific lesson. " +
                    "The new lesson number must be unique within the course and greater than zero."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson number updated successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid lesson number"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Lesson not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Lesson number already exists for the course"
    )
    @PatchMapping("/{uuid}/lesson-number")
    public ResponseEntity<ApiResponse<LessonDTO>> updateLessonNumber(
            @Parameter(description = "Unique identifier of the lesson", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "New lesson number", required = true)
            @RequestParam Integer newLessonNo) {
        LessonDTO updated = lessonService.updateLessonNumber(uuid, newLessonNo);
        return ResponseEntity.ok(ApiResponse.success(updated, "Lesson number updated successfully"));
    }

    @Operation(
            summary = "Delete a lesson",
            description = "Permanently deletes a lesson from the system. " +
                    "This operation will automatically reorder the remaining lessons in the course " +
                    "to maintain sequential numbering."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson deleted successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Lesson not found"
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @Parameter(description = "Unique identifier of the lesson to delete", required = true)
            @PathVariable UUID uuid) {
        lessonService.deleteLesson(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Lesson deleted successfully"));
    }

    @Operation(
            summary = "Bulk delete lessons",
            description = "Deletes multiple lessons in a single operation. " +
                    "Automatically reorders remaining lessons in affected courses to maintain sequential numbering."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lessons deleted successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "One or more lessons not found"
    )
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkDeleteLessons(
            @Parameter(description = "List of lesson UUIDs to delete", required = true)
            @RequestBody List<UUID> lessonUuids) {
        lessonService.bulkDeleteLessons(lessonUuids);
        return ResponseEntity.ok(ApiResponse.success(null, "Lessons deleted successfully"));
    }

    @Operation(
            summary = "Check if lesson exists",
            description = "Checks whether a lesson exists by its UUID. " +
                    "Useful for validation before performing operations."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Existence check completed"
    )
    @GetMapping("/{uuid}/exists")
    public ResponseEntity<ApiResponse<Boolean>> existsByUuid(
            @Parameter(description = "Unique identifier of the lesson", required = true)
            @PathVariable UUID uuid) {
        boolean exists = lessonService.existsByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(exists, "Lesson existence check completed"));
    }

    @Operation(
            summary = "Search lessons",
            description = "Searches for lessons based on multiple criteria. " +
                    "Supports flexible filtering and returns paginated results. " +
                    "Available search parameters: " +
                    "• 'name' - Search in lesson name " +
                    "• 'description' - Search in lesson description " +
                    "• 'courseUuid' - Filter by course UUID " +
                    "• 'type' - Filter by lesson type (TEXT, VIDEO, PDF, YOUTUBE_LINK) " +
                    "• 'minDuration' - Minimum duration in minutes " +
                    "• 'maxDuration' - Maximum duration in minutes " +
                    "• 'lessonNo' - Filter by lesson number " +
                    "• 'createdBy' - Filter by creator " +
                    "• 'term' - Global search term (searches name and description)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lesson search completed successfully"
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedDTO<LessonDTO>>> searchLessons(
            @Parameter(description = "Search parameters as key-value pairs")
            @RequestParam(required = false) Map<String, String> searchParams,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LessonDTO> lessons = lessonService.search(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(lessons, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Lesson search completed successfully"));
    }
}