package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "APIs for managing courses, their content, and associated metadata")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(
            summary = "Create a new course",
            description = "Creates a new course with the provided details including metadata, learning objectives, and categories"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Course created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class,
                                    subTypes = {CourseDTO.class}))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Course with the same name already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<CourseDTO>> createCourse(
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO createdCourse = courseService.createCourse(courseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdCourse, "Course created successfully"));
    }

    @GetMapping("/{uuid}")
    @Operation(
            summary = "Get course by UUID",
            description = "Retrieves a specific course by its UUID including all associated metadata, learning objectives, and categories"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Course found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class,
                                    subTypes = {CourseDTO.class}))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Course not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<CourseDTO>> getCourseByUuid(
            @Parameter(description = "UUID of the course to be retrieved", required = true)
            @PathVariable UUID uuid) {
        CourseDTO course = courseService.getCourseByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(course, "Course retrieved successfully"));
    }

    @GetMapping
    @Operation(
            summary = "Get all courses",
            description = "Retrieves all courses with pagination, including their metadata and associations"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of courses retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class,
                                    subTypes = {PagedDTO.class}))
            )
    })
    public ResponseEntity<ApiResponse<PagedDTO<CourseDTO>>> getAllCourses(
            @Parameter(description = "Pagination parameters (page, size, sort)")
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<CourseDTO> courses = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(courses, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Courses retrieved successfully"));
    }

    @PutMapping("/{uuid}")
    @Operation(
            summary = "Update a course",
            description = "Updates an existing course with the provided details. Partial updates are supported - only provided fields will be updated."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Course updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class,
                                    subTypes = {CourseDTO.class}))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Course not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Course with the same name already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<CourseDTO>> updateCourse(
            @Parameter(description = "UUID of the course to be updated", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO updatedCourse = courseService.updateCourse(uuid, courseDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedCourse, "Course updated successfully"));
    }

    @DeleteMapping("/{uuid}")
    @Operation(
            summary = "Delete a course",
            description = "Deletes a course by its UUID including all associated data such as categories and learning objectives"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Course deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Course not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<Void> deleteCourse(
            @Parameter(description = "UUID of the course to be deleted", required = true)
            @PathVariable UUID uuid) {
        courseService.deleteCourse(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search courses",
            description = "Search courses based on multiple criteria with pagination. Supports filtering by name, description, difficulty level, price range, and more."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of matching courses retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class,
                                    subTypes = {PagedDTO.class}))
            )
    })
    public ResponseEntity<ApiResponse<PagedDTO<CourseDTO>>> searchCourses(
            @Parameter(description = "Search parameters (name, description, difficultyLevel, minPrice, maxPrice, etc.)")
            @RequestParam(required = false) Map<String, String> searchParams,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<CourseDTO> courses = courseService.searchCourses(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(courses, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Courses search completed successfully"));
    }
}