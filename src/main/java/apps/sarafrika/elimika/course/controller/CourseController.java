package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.*;
        import apps.sarafrika.elimika.course.service.*;
        import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for comprehensive course management operations.
 */
@RestController
@RequestMapping(CourseController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Complete course lifecycle management including content, assessments, and analytics")
public class CourseController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CourseService courseService;
    private final LessonService lessonService;
    private final LessonContentService lessonContentService;
    private final CourseAssessmentService courseAssessmentService;
    private final CourseRequirementService courseRequirementService;
    private final CourseEnrollmentService courseEnrollmentService;

    // ===== COURSE BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new course",
            description = "Creates a new course with default DRAFT status and inactive state.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course created successfully",
                            content = @Content(schema = @Schema(implementation = CourseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseDTO>> createCourse(
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO createdCourse = courseService.createCourse(courseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdCourse, "Course created successfully"));
    }

    @Operation(
            summary = "Get course by UUID",
            description = "Retrieves a complete course profile including computed properties.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course found"),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseDTO>> getCourseByUuid(
            @PathVariable UUID uuid) {
        CourseDTO courseDTO = courseService.getCourseByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(courseDTO, "Course retrieved successfully"));
    }

    @Operation(
            summary = "Get all courses",
            description = "Retrieves paginated list of all courses with filtering support."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> getAllCourses(
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(courses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Courses retrieved successfully"));
    }

    @Operation(
            summary = "Update course",
            description = "Updates an existing course with selective field updates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseDTO>> updateCourse(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseDTO courseDTO) {
        CourseDTO updatedCourse = courseService.updateCourse(uuid, courseDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedCourse, "Course updated successfully"));
    }

    @Operation(
            summary = "Delete course",
            description = "Permanently removes a course and its associated data.",
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

    @Operation(
            summary = "Publish course",
            description = "Publishes a course making it available for enrollment.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course published successfully"),
                    @ApiResponse(responseCode = "400", description = "Course not ready for publishing")
            }
    )
    @PostMapping("/{uuid}/publish")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseDTO>> publishCourse(
            @PathVariable UUID uuid) {
        if (!courseService.isCourseReadyForPublishing(uuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.common.dto.ApiResponse
                            .error("Course is not ready for publishing.",
                                    null));
        }

        CourseDTO publishedCourse = courseService.publishCourse(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(publishedCourse, "Course published successfully"));
    }

    @Operation(
            summary = "Search courses",
            description = """
                    Advanced course search with flexible criteria and operators.
                    
                    **Common Course Search Examples:**
                    - `name_like=javascript` - Courses with names containing "javascript"
                    - `status=PUBLISHED` - Only published courses
                    - `active=true` - Only active courses
                    - `status_in=PUBLISHED,ACTIVE` - Published or active courses
                    - `price_lte=100.00` - Courses priced at $100 or less
                    - `price=null` - Free courses
                    - `instructorUuid=uuid` - Courses by specific instructor
                    - `categoryUuid=uuid` - Courses in specific category
                    - `difficultyUuid=uuid` - Courses of specific difficulty level
                    - `durationHours_gte=20` - Courses 20+ hours long
                    - `createdDate_gte=2024-01-01T00:00:00` - Courses created after Jan 1, 2024
                    
                    **Advanced Course Queries:**
                    - `status=PUBLISHED&active=true&price_lte=50` - Published, active courses under $50
                    - `name_like=python&difficultyUuid=beginner-uuid` - Beginner Python courses
                    - `instructorUuid=uuid&status=PUBLISHED` - Published courses by specific instructor
                    
                    For complete operator documentation, see the instructor search endpoint.
                    """
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> searchCourses(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(courses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course search completed successfully"));
    }

    // ===== COURSE LESSONS =====

    @Operation(
            summary = "Add lesson to course",
            description = "Creates a new lesson associated with the specified course."
    )
    @PostMapping("/{courseUuid}/lessons")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<LessonDTO>> addCourseLesson(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody LessonDTO lessonDTO) {
        LessonDTO createdLesson = lessonService.createLesson(lessonDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdLesson, "Lesson added successfully"));
    }

    @Operation(
            summary = "Get course lessons",
            description = "Retrieves all lessons for a specific course in sequence order."
    )
    @GetMapping("/{courseUuid}/lessons")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<LessonDTO>>> getCourseLessons(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<LessonDTO> lessons = lessonService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.common.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied - insufficient permissions to view lesson details",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.common.dto.ApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid UUID format provided",
                            content = @Content(schema = @Schema(implementation = apps.sarafrika.elimika.common.dto.ApiResponse.class))
                    )
            }
    )
    @GetMapping("/{courseUuid}/lessons/{lessonUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<LessonDTO>> getCourseLesson(
            @PathVariable("courseUuid")
            @Schema(description = "UUID of the course containing the lesson",
                    example = "123e4567-e89b-12d3-a456-426614174000")
            UUID courseUuid,

            @PathVariable("lessonUuid")
            @Schema(description = "UUID of the lesson to retrieve",
                    example = "987fcdeb-51a2-43d7-8f9e-123456789abc")
            UUID lessonUuid) {
        LessonDTO lesson = lessonService.getLessonByUuid(lessonUuid);

        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(lesson, "Lesson retrieved successfully"));
    }

    @Operation(
            summary = "Update course lesson",
            description = "Updates a specific lesson within a course."
    )
    @PutMapping("/{courseUuid}/lessons/{lessonUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<LessonDTO>> updateCourseLesson(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @Valid @RequestBody LessonDTO lessonDTO) {
        LessonDTO updatedLesson = lessonService.updateLesson(lessonUuid, lessonDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<LessonContentDTO>> addLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @Valid @RequestBody LessonContentDTO contentDTO) {
        LessonContentDTO createdContent = lessonContentService.createLessonContent(contentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdContent, "Content added successfully"));
    }

    @Operation(
            summary = "Get lesson content",
            description = "Retrieves all content for a lesson in display order with computed properties."
    )
    @GetMapping("/{courseUuid}/lessons/{lessonUuid}/content")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<List<LessonContentDTO>>> getLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid) {
        List<LessonContentDTO> content = lessonContentService.getContentByLesson(lessonUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(content, "Lesson content retrieved successfully"));
    }

    @Operation(
            summary = "Update lesson content",
            description = "Updates a specific content item within a lesson."
    )
    @PutMapping("/{courseUuid}/lessons/{lessonUuid}/content/{contentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<LessonContentDTO>> updateLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @PathVariable UUID contentUuid,
            @Valid @RequestBody LessonContentDTO contentDTO) {
        LessonContentDTO updatedContent = lessonContentService.updateLessonContent(contentUuid, contentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<String>> reorderLessonContent(
            @PathVariable UUID courseUuid,
            @PathVariable UUID lessonUuid,
            @RequestBody List<UUID> contentUuids) {
        lessonContentService.reorderContent(lessonUuid, contentUuids);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success("Content reordered successfully", "Lesson content order updated"));
    }

    // ===== COURSE ASSESSMENTS =====

    @Operation(
            summary = "Add assessment to course",
            description = "Creates a new assessment for the course with optional rubric association."
    )
    @PostMapping("/{courseUuid}/assessments")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseAssessmentDTO>> addCourseAssessment(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody CourseAssessmentDTO assessmentDTO) {
        CourseAssessmentDTO createdAssessment = courseAssessmentService.createCourseAssessment(assessmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdAssessment, "Assessment added successfully"));
    }

    @Operation(
            summary = "Get course assessments",
            description = "Retrieves all assessments for a specific course."
    )
    @GetMapping("/{courseUuid}/assessments")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseAssessmentDTO>>> getCourseAssessments(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseAssessmentDTO> assessments = courseAssessmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(assessments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course assessments retrieved successfully"));
    }

    @Operation(
            summary = "Update course assessment",
            description = "Updates a specific assessment within a course."
    )
    @PutMapping("/{courseUuid}/assessments/{assessmentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseAssessmentDTO>> updateCourseAssessment(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @Valid @RequestBody CourseAssessmentDTO assessmentDTO) {
        CourseAssessmentDTO updatedAssessment = courseAssessmentService.updateCourseAssessment(assessmentUuid, assessmentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseRequirementDTO>> addCourseRequirement(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody CourseRequirementDTO requirementDTO) {
        CourseRequirementDTO createdRequirement = courseRequirementService.createCourseRequirement(requirementDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdRequirement, "Requirement added successfully"));
    }

    @Operation(
            summary = "Get course requirements",
            description = "Retrieves all requirements for a specific course."
    )
    @GetMapping("/{courseUuid}/requirements")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseRequirementDTO>>> getCourseRequirements(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseRequirementDTO> requirements = courseRequirementService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(requirements, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course requirements retrieved successfully"));
    }

    @Operation(
            summary = "Update course requirement",
            description = "Updates a specific requirement for a course."
    )
    @PutMapping("/{courseUuid}/requirements/{requirementUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CourseRequirementDTO>> updateCourseRequirement(
            @PathVariable UUID courseUuid,
            @PathVariable UUID requirementUuid,
            @Valid @RequestBody CourseRequirementDTO requirementDTO) {
        CourseRequirementDTO updatedRequirement = courseRequirementService.updateCourseRequirement(requirementUuid, requirementDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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

    // ===== COURSE ENROLLMENTS =====

    @Operation(
            summary = "Get course enrollments",
            description = "Retrieves enrollment data for a specific course with analytics."
    )
    @GetMapping("/{courseUuid}/enrollments")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseEnrollmentDTO>>> getCourseEnrollments(
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        Page<CourseEnrollmentDTO> enrollments = courseEnrollmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(enrollments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Get course completion rate",
            description = "Returns the completion rate percentage for a course."
    )
    @GetMapping("/{courseUuid}/completion-rate")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<Double>> getCourseCompletionRate(
            @PathVariable UUID courseUuid) {
        double completionRate = courseService.getCourseCompletionRate(courseUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(completionRate, "Course completion rate retrieved successfully"));
    }

    // ===== COURSE ANALYTICS =====

    @Operation(
            summary = "Get active courses",
            description = "Retrieves all currently active and published courses."
    )
    @GetMapping("/active")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> getActiveCourses(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("active", "true");
        Page<CourseDTO> activeCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(activeCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Active courses retrieved successfully"));
    }

    @Operation(
            summary = "Get published courses",
            description = "Retrieves all published courses available for enrollment."
    )
    @GetMapping("/published")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> getPublishedCourses(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("status", "PUBLISHED");
        Page<CourseDTO> publishedCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(publishedCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Published courses retrieved successfully"));
    }

    @Operation(
            summary = "Get free courses",
            description = "Retrieves all courses available at no cost."
    )
    @GetMapping("/free")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> getFreeCourses(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("price", "null");
        Page<CourseDTO> freeCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(freeCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Free courses retrieved successfully"));
    }

    @Operation(
            summary = "Get courses by instructor",
            description = "Retrieves all courses created by a specific instructor."
    )
    @GetMapping("/instructor/{instructorUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> getCoursesByInstructor(
            @PathVariable UUID instructorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("instructorUuid", instructorUuid.toString());
        Page<CourseDTO> instructorCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(instructorCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Instructor courses retrieved successfully"));
    }

    @Operation(
            summary = "Get courses by category",
            description = "Retrieves all courses in a specific category."
    )
    @GetMapping("/category/{categoryUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseDTO>>> getCoursesByCategory(
            @PathVariable UUID categoryUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("categoryUuid", categoryUuid.toString());
        Page<CourseDTO> categoryCourses = courseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(categoryCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Category courses retrieved successfully"));
    }

    // ===== SEARCH ENDPOINTS FOR COURSE ENTITIES =====

    @Operation(
            summary = "Search lessons",
            description = """
                    Search course lessons with advanced filtering.
                    
                    **Common Lesson Search Examples:**
                    - `courseUuid=uuid` - All lessons for specific course
                    - `status=PUBLISHED` - Only published lessons
                    - `active=true` - Only active lessons
                    - `lessonNumber_gte=5` - Lessons from lesson 5 onwards
                    - `title_like=introduction` - Lessons with "introduction" in title
                    - `durationHours_between=1,3` - Lessons between 1-3 hours
                    """
    )
    @GetMapping("/lessons/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<LessonDTO>>> searchLessons(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<LessonDTO> lessons = lessonService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(lessons, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Lesson search completed successfully"));
    }

    @Operation(
            summary = "Search lesson content",
            description = """
                    Search lesson content across all courses.
                    
                    **Common Content Search Examples:**
                    - `lessonUuid=uuid` - All content for specific lesson
                    - `contentTypeUuid=uuid` - Content of specific type
                    - `isRequired=true` - Only required content
                    - `title_like=video` - Content with "video" in title
                    - `fileSizeBytes_gt=1048576` - Files larger than 1MB
                    """
    )
    @GetMapping("/content/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<LessonContentDTO>>> searchLessonContent(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<LessonContentDTO> content = lessonContentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(content, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Content search completed successfully"));
    }

    @Operation(
            summary = "Search course assessments",
            description = """
                    Search assessments across all courses.
                    
                    **Common Assessment Search Examples:**
                    - `courseUuid=uuid` - All assessments for specific course
                    - `assessmentType=QUIZ` - Only quiz assessments
                    - `isRequired=true` - Only required assessments
                    - `weightPercentage_gte=20` - Assessments worth 20% or more
                    """
    )
    @GetMapping("/assessments/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseAssessmentDTO>>> searchAssessments(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseAssessmentDTO> assessments = courseAssessmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(assessments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Assessment search completed successfully"));
    }

    @Operation(
            summary = "Search course requirements",
            description = """
                    Search course requirements and prerequisites.
                    
                    **Common Requirement Search Examples:**
                    - `courseUuid=uuid` - All requirements for specific course
                    - `requirementType=PREREQUISITE` - Only prerequisites
                    - `isMandatory=true` - Only mandatory requirements
                    - `requirementText_like=experience` - Requirements mentioning "experience"
                    """
    )
    @GetMapping("/requirements/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseRequirementDTO>>> searchRequirements(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseRequirementDTO> requirements = courseRequirementService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(requirements, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Requirements search completed successfully"));
    }

    @Operation(
            summary = "Search course enrollments",
            description = """
                    Search enrollment records across all courses.
                    
                    **Common Enrollment Search Examples:**
                    - `courseUuid=uuid` - All enrollments for specific course
                    - `studentUuid=uuid` - All enrollments for specific student
                    - `status=COMPLETED` - Only completed enrollments
                    - `progressPercentage_gte=80` - Students with 80%+ progress
                    - `enrollmentDate_gte=2024-01-01T00:00:00` - Enrollments from 2024
                    """
    )
    @GetMapping("/enrollments/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CourseEnrollmentDTO>>> searchEnrollments(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseEnrollmentDTO> enrollments = courseEnrollmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(enrollments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Enrollments search completed successfully"));
    }
}