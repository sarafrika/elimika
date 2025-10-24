package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.service.*;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * REST Controller for comprehensive course management operations.
 */
@RestController
@RequestMapping(CourseController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Complete course lifecycle management including content, assessments, media, analytics, and category management")
public class CourseController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CourseService courseService;
    private final LessonService lessonService;
    private final LessonContentService lessonContentService;
    private final CourseAssessmentService courseAssessmentService;
    private final CourseRequirementService courseRequirementService;
    private final CourseTrainingRequirementService courseTrainingRequirementService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final CourseCategoryService courseCategoryService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    // ===== COURSE BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new course",
            description = """
                Creates a new course with default DRAFT status and inactive state. Supports multiple categories.
                
                **Category Assignment:**
                - Use `category_uuids` field to assign multiple categories to the course
                - Categories are validated to ensure they exist before assignment
                - A course can belong to multiple categories for better organization and discoverability
                
                **Example Request Body:**
                ```json
                {
                    "name": "Advanced Java Programming",
                    "instructor_uuid": "instructor-uuid-here",
                    "category_uuids": ["java-uuid", "programming-uuid"],
                    "description": "Comprehensive Java course",
                    "duration_hours": 40,
                    "duration_minutes": 0
                }
                ```
                """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course created successfully",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data or category not found")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> createCourse(
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO createdCourse = courseService.createCourse(courseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdCourse, "Course created successfully"));
    }

    @Operation(
            summary = "Get course by UUID",
            description = """
                Retrieves a complete course profile including computed properties and category information.
                
                **Response includes:**
                - All course details and metadata
                - `category_uuids`: List of category UUIDs the course belongs to
                - `category_names`: List of category names for display (read-only)
                - `category_count`: Number of categories assigned to the course
                - `has_multiple_categories`: Boolean indicating if course has multiple categories
                """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course found"),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> getCourseByUuid(
            @PathVariable UUID uuid) {
        CourseDTO courseDTO = courseService.getCourseByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(courseDTO, "Course retrieved successfully"));
    }

    @Operation(
            summary = "Get all courses",
            description = "Retrieves paginated list of all courses with category information and filtering support."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseDTO>>> getAllCourses(
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(courses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Courses retrieved successfully"));
    }

    @Operation(
            summary = "Update course",
            description = """
                Updates an existing course with selective field updates including category management.

                **Category Updates:**
                - Provide `category_uuids` to completely replace existing categories
                - To add categories, include existing + new category UUIDs
                - To remove all categories, provide an empty array
                - Changes to categories are applied atomically

                **Authorization:** Only the course owner can update the course.
                """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid category UUIDs provided"),
                    @ApiResponse(responseCode = "403", description = "Not authorized - only course owner can update")
            }
    )
    @PutMapping("/{uuid}")
    @org.springframework.security.access.prepost.PreAuthorize("@courseSecurityService.isCourseOwner(#uuid)")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> updateCourse(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO updatedCourse = courseService.updateCourse(uuid, courseDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCourse, "Course updated successfully"));
    }

    @Operation(
            summary = "Unpublish course",
            description = """
            Unpublishes a course, changing it from PUBLISHED to DRAFT status.
            
            **Smart Active Status Logic:**
            - If NO active enrollments: Course becomes DRAFT and ACTIVE (available for new enrollments)
            - If HAS active enrollments: Course becomes DRAFT and INACTIVE (existing students continue, no new enrollments)
            
            **Business Rules:**
            - Course status always changes from PUBLISHED to DRAFT
            - Active status depends on current enrollment situation
            - Existing enrollments are never affected
            - Course can be published again later
            
            **Use Cases:**
            - Temporarily remove course from catalog while keeping it available
            - Stop new enrollments while allowing current students to continue
            - Prepare course for updates before republishing
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course unpublished successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @PostMapping("/{uuid}/unpublish")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> unpublishCourse(
            @PathVariable UUID uuid) {

        CourseDTO unpublishedCourse = courseService.unpublishCourse(uuid);

        String message = Boolean.TRUE.equals(unpublishedCourse.active()) ?
                "Course unpublished successfully and remains active for new enrollments" :
                "Course unpublished successfully and set to inactive due to existing enrollments";

        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(unpublishedCourse, message));
    }

    @Operation(
            summary = "Archive course",
            description = """
            Archives a course, making it completely unavailable.
            
            **Important:**
            - This is typically a permanent action
            - Course becomes completely inaccessible to new students
            - Existing enrollments may be handled differently based on business rules
            - Course data is preserved for historical/audit purposes
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course archived successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @PostMapping("/{uuid}/archive")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> archiveCourse(
            @PathVariable UUID uuid) {

        CourseDTO archivedCourse = courseService.archiveCourse(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(archivedCourse, "Course archived successfully"));
    }

    @Operation(
            summary = "Get available status transitions",
            description = """
            Returns the list of valid status transitions for a course based on its current state and business rules.
            
            **Status Transition Rules:**
            - DRAFT → IN_REVIEW, ARCHIVED
            - IN_REVIEW → DRAFT, PUBLISHED, ARCHIVED  
            - PUBLISHED → DRAFT (if no active enrollments), ARCHIVED
            - ARCHIVED → (no transitions - permanent state)
            """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Available transitions retrieved successfully")
            }
    )
    @GetMapping("/{uuid}/status-transitions")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<ContentStatus>>> getStatusTransitions(
            @PathVariable UUID uuid) {

        List<ContentStatus> availableTransitions = courseService.getAvailableStatusTransitions(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(availableTransitions, "Available status transitions retrieved successfully"));
    }

    @Operation(
            summary = "Delete course",
            description = "Permanently removes a course, its category associations, and all associated data.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID uuid) {
        courseService.deleteCourse(uuid);
        return ResponseEntity.noContent().build();
    }

    // ===== COURSE CATEGORY MANAGEMENT =====

    @Operation(
            summary = "Get course categories",
            description = "Retrieves all categories assigned to a specific course."
    )
    @GetMapping("/{courseUuid}/categories")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseCategoryMappingDTO>>> getCourseCategories(
            @PathVariable UUID courseUuid) {
        List<CourseCategoryMappingDTO> mappings = courseCategoryService.getCourseCategoryMappings(courseUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(mappings, "Course categories retrieved successfully"));
    }

    @Operation(
            summary = "Remove category from course",
            description = "Removes a specific category from a course without affecting other categories."
    )
    @DeleteMapping("/{courseUuid}/categories/{categoryUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<String>> removeCategoryFromCourse(
            @PathVariable UUID courseUuid,
            @PathVariable UUID categoryUuid) {
        courseCategoryService.removeCategoryFromCourse(courseUuid, categoryUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success("Category removed from course successfully", "Category association deleted"));
    }

    @Operation(
            summary = "Remove all categories from course",
            description = "Removes all category associations from a course."
    )
    @DeleteMapping("/{courseUuid}/categories")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<String>> removeAllCategoriesFromCourse(
            @PathVariable UUID courseUuid) {
        long removedCount = courseCategoryService.getCategoryCountForCourse(courseUuid);
        courseCategoryService.removeAllCategoriesFromCourse(courseUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(String.format("Removed %d categories from course", removedCount),
                        "All category associations deleted"));
    }

    @Operation(
            summary = "Search courses with enhanced category filtering",
            description = """
                Advanced course search with flexible criteria and operators, including category-based filtering.
                
                **Category-Specific Search Examples:**
                - `categoryUuids_in=uuid1,uuid2` - Courses in any of these categories
                - `categoryUuids_contains=uuid` - Courses containing specific category
                - `categoryNames_like=programming` - Courses in categories with "programming" in the name
                - `categoryCount_gte=2` - Courses assigned to 2 or more categories
                - `hasMultipleCategories=true` - Courses with multiple category assignments
                
                **Combined Search Examples:**
                - `status=PUBLISHED&categoryUuids_in=uuid1,uuid2&price_lte=100` - Published courses under $100 in specific categories
                - `name_like=java&categoryNames_like=programming&active=true` - Active Java courses in programming categories
                
                For complete operator documentation, see the general course search endpoint.
                """
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseDTO>>> searchCourses(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(courses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course search completed successfully"));
    }

    @Operation(
            summary = "Publish course",
            description = "Publishes a course making it available for enrollment.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course published successfully"),
                    @ApiResponse(responseCode = "400", description = "Course not ready for publishing")
            }
    )
    @PostMapping("/{uuid}/publish")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> publishCourse(
            @PathVariable UUID uuid) {
        if (!courseService.isCourseReadyForPublishing(uuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                            .error("Course is not ready for publishing.",
                                    null));
        }

        CourseDTO publishedCourse = courseService.publishCourse(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(publishedCourse, "Course published successfully"));
    }

    // ===== COURSE MEDIA MANAGEMENT =====

    @Operation(
            summary = "Upload course thumbnail",
            description = """
                Uploads a thumbnail image for the specified course. The thumbnail is typically used in course 
                listings, search results, and course cards throughout the application.
                
                **File Requirements:**
                - Supported formats: JPG, PNG, GIF, WebP
                - Maximum file size: 5MB
                - Recommended dimensions: 400x300 pixels or 4:3 aspect ratio
                - Files will be automatically optimized for web delivery
                
                **Usage Guidelines:**
                - Thumbnails should clearly represent the course content
                - Use high-quality, professional images
                - Avoid images with too much text or small details
                - Consider accessibility and contrast for text overlays
                
                **Storage Details:**
                - Files are stored in the course_thumbnails folder
                - Previous thumbnail will be replaced if a new one is uploaded
                - Generated URL will be automatically set in the course record
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thumbnail uploaded successfully",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Course not found",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid file format or size exceeds limit",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    )
            }
    )
    @PostMapping(value = "/{uuid}/thumbnail", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> uploadCourseThumbnail(
            @Parameter(
                    description = "UUID of the course to upload thumbnail for. Must be an existing course identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001",
                    required = true
            )
            @PathVariable UUID uuid,

            @Parameter(
                    description = "Thumbnail image file to upload. Supported formats: JPG, PNG, GIF, WebP. Maximum size: 5MB.",
                    required = true
            )
            @RequestParam(value = "thumbnail", required = true) MultipartFile thumbnail) {

        CourseDTO updatedCourse = courseService.uploadThumbnail(uuid, thumbnail);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCourse, "Course thumbnail uploaded successfully"));
    }

    @Operation(
            summary = "Upload course banner",
            description = """
                Uploads a banner image for the specified course. The banner is typically used on the course 
                detail page as a hero image and in promotional materials.
                
                **File Requirements:**
                - Supported formats: JPG, PNG, GIF, WebP
                - Maximum file size: 10MB
                - Recommended dimensions: 1200x400 pixels or 3:1 aspect ratio
                - Files will be automatically optimized for web delivery
                
                **Usage Guidelines:**
                - Banners should be visually striking and professional
                - Consider responsive design - banner should work on mobile and desktop
                - Use images that complement your course branding
                - Ensure good contrast if overlaying text
                
                **Storage Details:**
                - Files are stored in the course_banners folder
                - Previous banner will be replaced if a new one is uploaded
                - Generated URL will be automatically set in the course record
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Banner uploaded successfully",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Course not found",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid file format or size exceeds limit",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    )
            }
    )
    @PostMapping(value = "/{uuid}/banner", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> uploadCourseBanner(
            @Parameter(
                    description = "UUID of the course to upload banner for. Must be an existing course identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001",
                    required = true
            )
            @PathVariable UUID uuid,

            @Parameter(
                    description = "Banner image file to upload. Supported formats: JPG, PNG, GIF, WebP. Maximum size: 10MB.",
                    required = true
            )
            @RequestParam(value = "banner", required = true) MultipartFile banner) {

        CourseDTO updatedCourse = courseService.uploadBanner(uuid, banner);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCourse, "Course banner uploaded successfully"));
    }

    @Operation(
            summary = "Upload course introduction video",
            description = """
                Uploads an introduction video for the specified course. The intro video is used for course 
                previews, marketing, and helping students understand what they'll learn.
                
                **File Requirements:**
                - Supported formats: MP4, WebM, MOV, AVI
                - Maximum file size: 100MB
                - Recommended duration: 1-3 minutes
                - Recommended resolution: 720p or 1080p
                
                **Content Guidelines:**
                - Keep intro videos concise and engaging
                - Clearly explain what students will learn
                - Include instructor introduction if appropriate
                - Ensure good audio quality
                - Consider adding captions for accessibility
                
                **Storage Details:**
                - Files are stored in the course_intro_videos folder
                - Previous intro video will be replaced if a new one is uploaded
                - Generated URL will be automatically set in the course record
                - Consider video compression for optimal streaming performance
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Introduction video uploaded successfully",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Course not found",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid file format or size exceeds limit",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    )
            }
    )
    @PostMapping(value = "/{uuid}/intro-video", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseDTO>> uploadCourseIntroVideo(
            @Parameter(
                    description = "UUID of the course to upload intro video for. Must be an existing course identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001",
                    required = true
            )
            @PathVariable UUID uuid,

            @Parameter(
                    description = "Introduction video file to upload. Supported formats: MP4, WebM, MOV, AVI. Maximum size: 100MB.",
                    required = true
            )
            @RequestParam(value = "intro_video", required = true) MultipartFile introVideo) {

        CourseDTO updatedCourse = courseService.uploadIntroVideo(uuid, introVideo);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCourse, "Course introduction video uploaded successfully"));
    }

    @Operation(
            summary = "Get course media by file name",
            description = """
                Retrieves course media files (thumbnails, banners, intro videos) by their file name. 
                This endpoint serves the actual media files with appropriate content types and caching headers.
                
                **File Types Served:**
                - Course thumbnails from course_thumbnails folder
                - Course banners from course_banners folder  
                - Course intro videos from course_intro_videos folder
                
                **Response Features:**
                - Automatic content type detection
                - Optimized caching headers for performance
                - Support for range requests (for videos)
                - Proper file serving with inline disposition
                
                **Usage:**
                - File names are typically returned from upload endpoints
                - URLs are automatically generated and stored in course records
                - Direct access via this endpoint for custom implementations
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Media file retrieved successfully",
                            content = @Content(mediaType = "application/octet-stream")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Media file not found",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    )
            }
    )
    @GetMapping("/media/{fileName}")
    public ResponseEntity<Resource> getCourseMedia(
            @Parameter(
                    description = "Name of the media file to retrieve. This is typically returned from the upload endpoints.",
                    example = "course_thumbnails_550e8400-e29b-41d4-a716-446655440001.jpg",
                    required = true
            )
            @PathVariable String fileName) {

        try {
            String fullPath = determineMediaPath(fileName);

            Resource resource = storageService.load(fullPath);
            String contentType = storageService.getContentType(fullPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== COURSE LESSONS =====

    @Operation(
            summary = "Add lesson to course",
            description = "Creates a new lesson associated with the specified course."
    )
    @PostMapping("/{courseUuid}/lessons")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<LessonDTO>> addCourseLesson(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody LessonDTO lessonDTO) {
        LessonDTO createdLesson = lessonService.createLesson(lessonDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdLesson, "Lesson added successfully"));
    }

    @Operation(
            summary = "Get course lessons",
            description = "Retrieves all lessons for a specific course in sequence order."
    )
    @GetMapping("/{courseUuid}/lessons")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<LessonDTO>>> getCourseLessons(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<LessonDTO> lessons = lessonService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(lessons, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course lessons retrieved successfully"));
    }

    @Operation(
            summary = "Get lesson by UUID",
            description = """
                Retrieves a specific lesson by its UUID within a course context.
                
                **Lesson Retrieval Details:**
                - Returns complete lesson profile including computed properties
                - Validates that the lesson belongs to the specified course
                - Includes lesson content count and duration calculations
                - Provides lesson status and completion tracking information
                
                **Response includes:**
                - Basic lesson information (title, description, objectives)
                - Lesson metadata (duration, sequence number, status)
                - Associated course UUID validation
                - Content summary statistics
                - Computed properties (isCompleted, progressPercentage for authenticated users)
                
                **Use Cases:**
                - Direct lesson navigation from course content
                - Lesson detail page rendering
                - Progress tracking and analytics
                - Content validation and prerequisites checking
                
                **Security Considerations:**
                - Validates lesson belongs to specified course
                - Respects course enrollment status for detailed information
                - May return limited data for unenrolled users depending on course visibility settings
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lesson retrieved successfully",
                            content = @Content(schema = @Schema(implementation = LessonDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Lesson not found or does not belong to the specified course",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied - insufficient permissions to view lesson details",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid UUID format provided",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.shared.dto.ApiResponse.class))
                    )
            }
    )
    @GetMapping("/{courseUuid}/lessons/{lessonUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<LessonDTO>> getCourseLesson(
            @PathVariable("courseUuid")
            @Schema(description = "UUID of the course containing the lesson",
                    example = "123e4567-e89b-12d3-a456-426614174000")
            UUID courseUuid,

            @PathVariable("lessonUuid")
            @Schema(description = "UUID of the lesson to retrieve",
                    example = "987fcdeb-51a2-43d7-8f9e-123456789abc")
            UUID lessonUuid) {
        LessonDTO lesson = lessonService.getLessonByUuid(lessonUuid);

        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(lesson, "Lesson retrieved successfully"));
    }

    @Operation(
            summary = "Update course lesson",
            description = "Updates a specific lesson within a course."
    )
    @PutMapping("/{courseUuid}/lessons/{lessonUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<LessonDTO>> updateCourseLesson(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @Valid @RequestBody LessonDTO lessonDTO) {
        LessonDTO updatedLesson = lessonService.updateLesson(lessonUuid, lessonDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedLesson, "Lesson updated successfully"));
    }

    @Operation(
            summary = "Delete course lesson",
            description = "Removes a lesson from a course including all associated content."
    )
    @DeleteMapping("/{courseUuid}/lessons/{lessonUuid}")
    public ResponseEntity<Void> deleteCourseLesson(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid) {
        lessonService.deleteLesson(lessonUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== LESSON CONTENT =====

    @Operation(
            summary = "Add content to lesson",
            description = "Adds new content item to a specific lesson with automatic ordering."
    )
    @PostMapping("/{courseUuid}/lessons/{lessonUuid}/content")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<LessonContentDTO>> addLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @Valid @RequestBody LessonContentDTO contentDTO) {
        LessonContentDTO createdContent = lessonContentService.createLessonContent(contentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdContent, "Content added successfully"));
    }

    @Operation(
            summary = "Get lesson content",
            description = "Retrieves all content for a lesson in display order with computed properties."
    )
    @GetMapping("/{courseUuid}/lessons/{lessonUuid}/content")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<LessonContentDTO>>> getLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid) {
        List<LessonContentDTO> content = lessonContentService.getContentByLesson(lessonUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(content, "Lesson content retrieved successfully"));
    }

    @Operation(
            summary = "Update lesson content",
            description = "Updates a specific content item within a lesson."
    )
    @PutMapping("/{courseUuid}/lessons/{lessonUuid}/content/{contentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<LessonContentDTO>> updateLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @PathVariable UUID contentUuid,
            @Valid @RequestBody LessonContentDTO contentDTO) {
        LessonContentDTO updatedContent = lessonContentService.updateLessonContent(contentUuid, contentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedContent, "Content updated successfully"));
    }

    @Operation(
            summary = "Delete lesson content",
            description = "Removes content from a lesson."
    )
    @DeleteMapping("/{courseUuid}/lessons/{lessonUuid}/content/{contentUuid}")
    public ResponseEntity<Void> deleteLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @PathVariable UUID contentUuid) {
        lessonContentService.deleteLessonContent(contentUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reorder lesson content",
            description = "Updates the display order of content items within a lesson."
    )
    @PostMapping("/{courseUuid}/lessons/{lessonUuid}/content/reorder")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<String>> reorderLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @RequestBody List<UUID> contentUuids) {
        lessonContentService.reorderContent(lessonUuid, contentUuids);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success("Content reordered successfully", "Lesson content order updated"));
    }

    // ===== COURSE ASSESSMENTS =====

    @Operation(
            summary = "Add assessment to course",
            description = "Creates a new assessment for the course with optional rubric association."
    )
    @PostMapping("/{courseUuid}/assessments")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentDTO>> addCourseAssessment(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody CourseAssessmentDTO assessmentDTO) {
        CourseAssessmentDTO createdAssessment = courseAssessmentService.createCourseAssessment(assessmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdAssessment, "Assessment added successfully"));
    }

    @Operation(
            summary = "Get course assessments",
            description = "Retrieves all assessments for a specific course."
    )
    @GetMapping("/{courseUuid}/assessments")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseAssessmentDTO>>> getCourseAssessments(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseAssessmentDTO> assessments = courseAssessmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(assessments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course assessments retrieved successfully"));
    }

    @Operation(
            summary = "Update course assessment",
            description = "Updates a specific assessment within a course."
    )
    @PutMapping("/{courseUuid}/assessments/{assessmentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentDTO>> updateCourseAssessment(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @Valid @RequestBody CourseAssessmentDTO assessmentDTO) {
        CourseAssessmentDTO updatedAssessment = courseAssessmentService.updateCourseAssessment(assessmentUuid, assessmentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedAssessment, "Assessment updated successfully"));
    }

    @Operation(
            summary = "Delete course assessment",
            description = "Removes an assessment from a course."
    )
    @DeleteMapping("/{courseUuid}/assessments/{assessmentUuid}")
    public ResponseEntity<Void> deleteCourseAssessment(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid) {
        courseAssessmentService.deleteCourseAssessment(assessmentUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== COURSE REQUIREMENTS =====

    @Operation(
            summary = "Add requirement to course",
            description = "Adds a new requirement or prerequisite to a course."
    )
    @PostMapping("/{courseUuid}/requirements")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseRequirementDTO>> addCourseRequirement(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody CourseRequirementDTO requirementDTO) {
        CourseRequirementDTO createdRequirement = courseRequirementService.createCourseRequirement(requirementDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdRequirement, "Requirement added successfully"));
    }

    @Operation(
            summary = "Get course requirements",
            description = "Retrieves all requirements for a specific course."
    )
    @GetMapping("/{courseUuid}/requirements")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseRequirementDTO>>> getCourseRequirements(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseRequirementDTO> requirements = courseRequirementService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(requirements, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course requirements retrieved successfully"));
    }

    @Operation(
            summary = "Update course requirement",
            description = "Updates a specific requirement for a course."
    )
    @PutMapping("/{courseUuid}/requirements/{requirementUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseRequirementDTO>> updateCourseRequirement(
            @PathVariable UUID courseUuid,
            @PathVariable UUID requirementUuid,
            @Valid @RequestBody CourseRequirementDTO requirementDTO) {
        CourseRequirementDTO updatedRequirement = courseRequirementService.updateCourseRequirement(requirementUuid, requirementDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedRequirement, "Requirement updated successfully"));
    }

    @Operation(
            summary = "Delete course requirement",
            description = "Removes a requirement from a course."
    )
    @DeleteMapping("/{courseUuid}/requirements/{requirementUuid}")
    public ResponseEntity<Void> deleteCourseRequirement(
            @PathVariable UUID courseUuid,
            @PathVariable UUID requirementUuid) {
        courseRequirementService.deleteCourseRequirement(requirementUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== COURSE TRAINING REQUIREMENTS =====

    @Operation(
            summary = "Add training delivery requirement",
            description = "Adds a new material, equipment, or facility requirement necessary to deliver the course."
    )
    @PostMapping("/{courseUuid}/training-requirements")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseTrainingRequirementDTO>> addCourseTrainingRequirement(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody CourseTrainingRequirementDTO requirementDTO) {
        CourseTrainingRequirementDTO createdRequirement = courseTrainingRequirementService.create(courseUuid, requirementDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdRequirement, "Training requirement added successfully"));
    }

    @Operation(
            summary = "Get training delivery requirements",
            description = "Retrieves all operational training requirements for a specific course."
    )
    @GetMapping("/{courseUuid}/training-requirements")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseTrainingRequirementDTO>>> getCourseTrainingRequirements(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseTrainingRequirementDTO> requirements = courseTrainingRequirementService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(requirements, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course training requirements retrieved successfully"));
    }

    @Operation(
            summary = "Update training delivery requirement",
            description = "Updates a specific training delivery requirement for a course."
    )
    @PutMapping("/{courseUuid}/training-requirements/{requirementUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseTrainingRequirementDTO>> updateCourseTrainingRequirement(
            @PathVariable UUID courseUuid,
            @PathVariable UUID requirementUuid,
            @Valid @RequestBody CourseTrainingRequirementDTO requirementDTO) {
        CourseTrainingRequirementDTO updatedRequirement = courseTrainingRequirementService.update(courseUuid, requirementUuid, requirementDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedRequirement, "Training requirement updated successfully"));
    }

    @Operation(
            summary = "Delete training delivery requirement",
            description = "Removes a training delivery requirement from a course."
    )
    @DeleteMapping("/{courseUuid}/training-requirements/{requirementUuid}")
    public ResponseEntity<Void> deleteCourseTrainingRequirement(
            @PathVariable UUID courseUuid,
            @PathVariable UUID requirementUuid) {
        courseTrainingRequirementService.delete(courseUuid, requirementUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== COURSE ENROLLMENTS =====

    @Operation(
            summary = "Get course enrollments",
            description = "Retrieves enrollment data for a specific course with analytics."
    )
    @GetMapping("/{courseUuid}/enrollments")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseEnrollmentDTO>>> getCourseEnrollments(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseEnrollmentDTO> enrollments = courseEnrollmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(enrollments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Get course completion rate",
            description = "Returns the completion rate percentage for a course."
    )
    @GetMapping("/{courseUuid}/completion-rate")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Double>> getCourseCompletionRate(
            @PathVariable UUID courseUuid) {
        double completionRate = courseService.getCourseCompletionRate(courseUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(completionRate, "Course completion rate retrieved successfully"));
    }

    // ===== COURSE ANALYTICS =====

    @Operation(
            summary = "Get active courses",
            description = "Retrieves all currently active and published courses."
    )
    @GetMapping("/active")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseDTO>>> getActiveCourses(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("active", "true");
        Page<CourseDTO> activeCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(activeCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Active courses retrieved successfully"));
    }

    @Operation(
            summary = "Get published courses",
            description = "Retrieves all published courses available for enrollment."
    )
    @GetMapping("/published")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseDTO>>> getPublishedCourses(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("status", "PUBLISHED");
        Page<CourseDTO> publishedCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(publishedCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Published courses retrieved successfully"));
    }

    @Operation(
            summary = "Get courses by instructor",
            description = "Retrieves all courses created by a specific instructor."
    )
    @GetMapping("/instructor/{instructorUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseDTO>>> getCoursesByInstructor(
            @PathVariable UUID instructorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("instructorUuid", instructorUuid.toString());
        Page<CourseDTO> instructorCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(instructorCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Instructor courses retrieved successfully"));
    }

    @Operation(
            summary = "Get courses by category",
            description = """
                Retrieves all courses in a specific category.
                
                **Enhanced Category Search:**
                This endpoint now supports the many-to-many relationship, returning courses that have 
                the specified category assigned to them, regardless of what other categories they may also have.
                """
    )
    @GetMapping("/category/{categoryUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseDTO>>> getCoursesByCategory(
            @PathVariable UUID categoryUuid,
            Pageable pageable) {

        // Get course UUIDs for this category from the mapping service
        List<UUID> courseUuids = courseCategoryService.getCourseUuidsByCategory(categoryUuid);

        if (courseUuids.isEmpty()) {
            // Return empty page if no courses found for this category
            Page<CourseDTO> emptyCourses = Page.empty(pageable);
            return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                    .success(PagedDTO.from(emptyCourses, ServletUriComponentsBuilder
                                    .fromCurrentRequestUri().build().toString()),
                            "No courses found for this category"));
        }

        // Convert UUIDs to comma-separated string for search
        String courseUuidsList = courseUuids.stream()
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(","));

        Map<String, String> searchParams = Map.of("uuid_in", courseUuidsList);
        Page<CourseDTO> categoryCourses = courseService.search(searchParams, pageable);

        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(categoryCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Category courses retrieved successfully"));
    }

    /**
     * Determines the full storage path for a course media file
     * This method tries different folders based on file patterns or types
     */
    private String determineMediaPath(String fileName) {
        String lowerFileName = fileName.toLowerCase();

        if (lowerFileName.contains("thumbnail")) {
            return storageProperties.getFolders().getCourseThumbnails() + "/" + fileName;
        } else if (lowerFileName.contains("banner")) {
            return storageProperties.getFolders().getCourseThumbnails() + "/" + fileName; // Using same folder
        } else if (storageService.isVideo(fileName)) {
            return storageProperties.getFolders().getCourseMaterials() + "/" + fileName;
        } else if (storageService.isImage(fileName)) {
            return storageProperties.getFolders().getCourseThumbnails() + "/" + fileName;
        } else {
            return storageProperties.getFolders().getCourseMaterials() + "/" + fileName;
        }
    }
}
