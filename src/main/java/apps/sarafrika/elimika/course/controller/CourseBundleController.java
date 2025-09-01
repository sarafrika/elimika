package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.CourseBundleDTO;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.service.CourseBundleService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Course Bundle Management
 * <p>
 * Provides comprehensive CRUD operations and lifecycle management for course bundles,
 * including independent pricing, content organization, and publication workflow.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/api/v1/course-bundles")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Course Bundle Management",
        description = "Comprehensive course bundle management with independent pricing and lifecycle controls"
)
public class CourseBundleController {

    public static final String API_ROOT_PATH = "/api/v1/course-bundles";

    private final CourseBundleService courseBundleService;

    // Basic CRUD Operations

    @Operation(
            summary = "Create Course Bundle",
            description = "Creates a new course bundle with independent pricing and lifecycle management."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Course bundle created successfully",
                    content = @Content(schema = @Schema(implementation = CourseBundleDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid bundle data provided"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied - instructor can only create bundles with their own courses"
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> createCourseBundle(
            @Valid @RequestBody CourseBundleDTO courseBundleDTO) {
        log.info("Creating course bundle: {}", courseBundleDTO.name());
        CourseBundleDTO createdBundle = courseBundleService.createCourseBundle(courseBundleDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdBundle, "Course bundle created successfully"));
    }

    @Operation(
            summary = "Get Course Bundle",
            description = "Retrieves a specific course bundle by UUID with all associated metadata."
    )
    @GetMapping("/{bundleUuid}")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> getCourseBundleByUuid(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.debug("Fetching course bundle: {}", bundleUuid);
        CourseBundleDTO bundle = courseBundleService.getCourseBundleByUuid(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(bundle, "Course bundle retrieved successfully"));
    }

    @Operation(
            summary = "List All Course Bundles",
            description = "Retrieves paginated list of all course bundles with filtering and sorting support."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<CourseBundleDTO>>> getAllCourseBundles(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching all course bundles");
        Page<CourseBundleDTO> bundles = courseBundleService.getAllCourseBundles(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(bundles, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Course bundles retrieved successfully"));
    }

    @Operation(
            summary = "Update Course Bundle",
            description = "Updates an existing course bundle. Only bundle owners can modify their bundles."
    )
    @PutMapping("/{bundleUuid}")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> updateCourseBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid,
            @Valid @RequestBody CourseBundleDTO courseBundleDTO) {
        log.info("Updating course bundle: {}", bundleUuid);
        CourseBundleDTO updatedBundle = courseBundleService.updateCourseBundle(bundleUuid, courseBundleDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedBundle, "Course bundle updated successfully"));
    }

    @Operation(
            summary = "Delete Course Bundle",
            description = "Deletes a course bundle and all associated course mappings. Only owners can delete their bundles."
    )
    @DeleteMapping("/{bundleUuid}")
    @PreAuthorize("@courseBundleSecurityService.canDeleteBundle(#bundleUuid)")
    public ResponseEntity<Void> deleteCourseBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.info("Deleting course bundle: {}", bundleUuid);
        courseBundleService.deleteCourseBundle(bundleUuid);
        return ResponseEntity.noContent().build();
    }

    // Search and Filtering

    @Operation(
            summary = "Search Course Bundles",
            description = "Advanced search for course bundles with multiple filter criteria and full-text search."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedDTO<CourseBundleDTO>>> searchCourseBundles(
            @Parameter(description = "Search parameters") @RequestParam Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Searching course bundles with params: {}", searchParams);
        Page<CourseBundleDTO> bundles = courseBundleService.search(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(bundles, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Course bundles search completed"));
    }

    @Operation(
            summary = "Get Published Course Bundles",
            description = "Retrieves all published and active course bundles available for student purchase."
    )
    @GetMapping("/published")
    public ResponseEntity<ApiResponse<PagedDTO<CourseBundleDTO>>> getPublishedCourseBundles(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching published course bundles");
        Page<CourseBundleDTO> bundles = courseBundleService.getPublishedCourseBundles(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(bundles, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Published course bundles retrieved successfully"));
    }

    @Operation(
            summary = "Get Instructor's Course Bundles",
            description = "Retrieves all course bundles created by a specific instructor."
    )
    @GetMapping("/instructor/{instructorUuid}")
    public ResponseEntity<ApiResponse<PagedDTO<CourseBundleDTO>>> getCourseBundlesByInstructor(
            @Parameter(description = "Instructor UUID") @PathVariable UUID instructorUuid,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching course bundles for instructor: {}", instructorUuid);
        Page<CourseBundleDTO> bundles = courseBundleService.getCourseBundlesByInstructor(instructorUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(bundles, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()), "Instructor course bundles retrieved successfully"));
    }

    // Course Management within Bundles

    @Operation(
            summary = "Get Bundle Courses",
            description = "Retrieves all courses included in a specific bundle, ordered by sequence."
    )
    @GetMapping("/{bundleUuid}/courses")
    public ResponseEntity<ApiResponse<List<CourseDTO>>> getBundleCourses(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.debug("Fetching courses for bundle: {}", bundleUuid);
        List<CourseDTO> courses = courseBundleService.getBundleCourses(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(courses, "Bundle courses retrieved successfully"));
    }

    @Operation(
            summary = "Add Course to Bundle",
            description = "Associates a course with a bundle. Only bundle owners can add courses they own."
    )
    @PostMapping("/{bundleUuid}/courses/{courseUuid}")
    @PreAuthorize("@courseBundleSecurityService.canAddCourseToBundle(#bundleUuid, #courseUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> addCourseToBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid,
            @Parameter(description = "Course UUID") @PathVariable UUID courseUuid,
            @Parameter(description = "Sequence order") @RequestParam(required = false) Integer sequenceOrder,
            @Parameter(description = "Is required course") @RequestParam(required = false, defaultValue = "true") Boolean isRequired) {
        log.info("Adding course {} to bundle {}", courseUuid, bundleUuid);
        CourseBundleDTO updatedBundle = courseBundleService.addCourseToBundle(bundleUuid, courseUuid, sequenceOrder, isRequired);
        return ResponseEntity.ok(ApiResponse.success(updatedBundle, "Course added to bundle successfully"));
    }

    @Operation(
            summary = "Remove Course from Bundle",
            description = "Removes a course association from a bundle. Only bundle owners can modify their bundles."
    )
    @DeleteMapping("/{bundleUuid}/courses/{courseUuid}")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> removeCourseFromBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid,
            @Parameter(description = "Course UUID") @PathVariable UUID courseUuid) {
        log.info("Removing course {} from bundle {}", courseUuid, bundleUuid);
        CourseBundleDTO updatedBundle = courseBundleService.removeCourseFromBundle(bundleUuid, courseUuid);
        return ResponseEntity.ok(ApiResponse.success(updatedBundle, "Course removed from bundle successfully"));
    }

    // Lifecycle Management

    @Operation(
            summary = "Check Bundle Publishing Readiness",
            description = "Validates if a bundle meets all requirements for publication."
    )
    @GetMapping("/{bundleUuid}/ready-for-publishing")
    @PreAuthorize("@courseBundleSecurityService.canViewBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<Boolean>> isBundleReadyForPublishing(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.debug("Checking publishing readiness for bundle: {}", bundleUuid);
        boolean isReady = courseBundleService.isBundleReadyForPublishing(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(isReady, 
                isReady ? "Bundle is ready for publishing" : "Bundle is not ready for publishing"));
    }

    @Operation(
            summary = "Publish Course Bundle",
            description = "Publishes a draft bundle, making it available for student purchase."
    )
    @PostMapping("/{bundleUuid}/publish")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> publishBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.info("Publishing bundle: {}", bundleUuid);
        CourseBundleDTO publishedBundle = courseBundleService.publishBundle(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(publishedBundle, "Course bundle published successfully"));
    }

    @Operation(
            summary = "Unpublish Course Bundle",
            description = "Reverts a published bundle back to draft status."
    )
    @PostMapping("/{bundleUuid}/unpublish")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> unpublishBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.info("Unpublishing bundle: {}", bundleUuid);
        CourseBundleDTO unpublishedBundle = courseBundleService.unpublishBundle(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(unpublishedBundle, "Course bundle unpublished successfully"));
    }

    @Operation(
            summary = "Archive Course Bundle",
            description = "Archives a bundle, making it unavailable for new purchases while preserving existing access."
    )
    @PostMapping("/{bundleUuid}/archive")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> archiveBundle(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.info("Archiving bundle: {}", bundleUuid);
        CourseBundleDTO archivedBundle = courseBundleService.archiveBundle(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(archivedBundle, "Course bundle archived successfully"));
    }

    @Operation(
            summary = "Get Available Status Transitions",
            description = "Retrieves all valid status transitions available for a bundle in its current state."
    )
    @GetMapping("/{bundleUuid}/status-transitions")
    @PreAuthorize("@courseBundleSecurityService.canViewBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<List<ContentStatus>>> getAvailableStatusTransitions(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.debug("Fetching status transitions for bundle: {}", bundleUuid);
        List<ContentStatus> transitions = courseBundleService.getAvailableStatusTransitions(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(transitions, "Status transitions retrieved successfully"));
    }

    @Operation(
            summary = "Validate Bundle Content",
            description = "Validates that all courses in the bundle are properly configured and published."
    )
    @GetMapping("/{bundleUuid}/validate")
    @PreAuthorize("@courseBundleSecurityService.canViewBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<Boolean>> validateBundleContent(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid) {
        log.debug("Validating content for bundle: {}", bundleUuid);
        boolean isValid = courseBundleService.validateBundleContent(bundleUuid);
        return ResponseEntity.ok(ApiResponse.success(isValid,
                isValid ? "Bundle content is valid" : "Bundle content has validation issues"));
    }

    // Media Upload Operations

    @Operation(
            summary = "Upload Bundle Thumbnail",
            description = "Uploads a thumbnail image for the course bundle."
    )
    @PostMapping("/{bundleUuid}/thumbnail")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> uploadThumbnail(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid,
            @Parameter(description = "Thumbnail image file") @RequestParam("thumbnail") MultipartFile thumbnail) {
        log.info("Uploading thumbnail for bundle: {}", bundleUuid);
        CourseBundleDTO updatedBundle = courseBundleService.uploadThumbnail(bundleUuid, thumbnail);
        return ResponseEntity.ok(ApiResponse.success(updatedBundle, "Thumbnail uploaded successfully"));
    }

    @Operation(
            summary = "Upload Bundle Banner",
            description = "Uploads a banner image for the course bundle."
    )
    @PostMapping("/{bundleUuid}/banner")
    @PreAuthorize("@courseBundleSecurityService.canModifyBundle(#bundleUuid)")
    public ResponseEntity<ApiResponse<CourseBundleDTO>> uploadBanner(
            @Parameter(description = "Course bundle UUID") @PathVariable UUID bundleUuid,
            @Parameter(description = "Banner image file") @RequestParam("banner") MultipartFile banner) {
        log.info("Uploading banner for bundle: {}", bundleUuid);
        CourseBundleDTO updatedBundle = courseBundleService.uploadBanner(bundleUuid, banner);
        return ResponseEntity.ok(ApiResponse.success(updatedBundle, "Banner uploaded successfully"));
    }
}