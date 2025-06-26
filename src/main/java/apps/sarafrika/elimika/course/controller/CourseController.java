package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Course Controller
 * <p>
 * REST API controller for course management operations in the Sarafrika Elimika system.
 * Provides endpoints for creating, retrieving, updating, deleting, and searching courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
@RestController
@RequestMapping("api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses API", description = "Course management and educational content operations")
public class CourseController {

    private final CourseService courseService;

    private final StorageService storageService;

    @Operation(
            summary = "Create a new course",
            description = "Creates a new course in the system with the provided course information. " +
                    "The course code must be unique across the system."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Course created successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation errors"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Course with the same code already exists"
    )
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CourseDTO>> createCourse(
            @Parameter(description = "Course information to create", required = true)
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO created = courseService.createCourse(courseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Course created successfully"));
    }

    @Operation(
            summary = "Get a course by UUID",
            description = "Retrieves a specific course by its unique identifier (UUID)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Course retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<CourseDTO>> getCourseByUuid(
            @Parameter(description = "Unique identifier of the course", required = true)
            @PathVariable UUID uuid) {
        return courseService.getCourseByUuid(uuid)
                .map(course -> ResponseEntity.ok(ApiResponse.success(course, "Course retrieved successfully")))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get all courses",
            description = "Retrieves a paginated list of all courses in the system. " +
                    "Supports pagination and sorting parameters."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Courses retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<CourseDTO>>> getAllCourses(
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CourseDTO> courses = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(courses, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Courses retrieved successfully"));
    }

    @Operation(
            summary = "Update a course",
            description = "Updates an existing course with the provided information. " +
                    "Only non-null fields in the request body will be updated."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Course updated successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation errors"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Course code conflict with existing course"
    )
    @PutMapping(value = "/{uuid}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CourseDTO>> updateCourse(
            @Parameter(description = "Unique identifier of the course to update", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Updated course information", required = true)
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO updated = courseService.updateCourse(uuid, courseDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Course updated successfully"));
    }

    @Operation(
            summary = "Delete a course",
            description = "Permanently deletes a course from the system. " +
                    "This operation is irreversible and will remove all associated data."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Course deleted successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Course not found"
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @Parameter(description = "Unique identifier of the course to delete", required = true)
            @PathVariable UUID uuid) {
        courseService.deleteCourse(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Course deleted successfully"));
    }

    @Operation(
            summary = "Search courses",
            description = "Searches for courses based on multiple criteria. " +
                    "Supports flexible filtering and returns paginated results. " +
                    "Available search parameters: " +
                    "• 'name' - Search in course name " +
                    "• 'description' - Search in course description " +
                    "• 'code' - Search by course code " +
                    "• 'status' - Filter by course status (ACTIVE, INACTIVE, ARCHIVED, DRAFT) " +
                    "• 'difficulty' - Filter by difficulty level " +
                    "• 'minPrice' - Minimum price filter " +
                    "• 'maxPrice' - Maximum price filter " +
                    "• 'minAge' - Minimum age eligibility " +
                    "• 'maxAge' - Maximum age eligibility " +
                    "• 'createdBy' - Filter by creator " +
                    "• 'term' - Global search term (searches name, description, and code)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Course search completed successfully"
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedDTO<CourseDTO>>> searchCourses(
            @Parameter(description = "Search parameters as key-value pairs")
            @RequestParam(required = false) Map<String, String> searchParams,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CourseDTO> courses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(courses, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Course search completed successfully"));
    }

    @Operation(summary = "Get course thumbnail image by file name")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course thumbnail retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course thumbnail not found")
    @GetMapping("thumbnail/{fileName}")
    public ResponseEntity<Resource> getCourseThumbnail(@PathVariable String fileName) {
        return ResponseEntity.ok().body(storageService.load(fileName));
    }
}