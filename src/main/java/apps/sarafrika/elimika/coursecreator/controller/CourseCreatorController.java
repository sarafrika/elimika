package apps.sarafrika.elimika.coursecreator.controller;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorService;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
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

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing course creator operations.
 * Provides endpoints for CRUD operations, search, and verification management.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-09-30
 */
@RestController
@RequestMapping(CourseCreatorController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Creator Management", description = "Comprehensive endpoints for managing course creators and their verification status")
public class CourseCreatorController {

    public static final String API_ROOT_PATH = "/api/v1/course-creators";

    private final CourseCreatorService courseCreatorService;

    // ===== COURSE CREATOR BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new course creator",
            description = "Saves a new course creator profile in the system. The course creator will be unverified by default and require admin verification.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course creator created successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> createCourseCreator(
            @Valid @RequestBody CourseCreatorDTO courseCreatorDTO) {
        CourseCreatorDTO createdCourseCreator = courseCreatorService.createCourseCreator(courseCreatorDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdCourseCreator, "Course creator created successfully"));
    }

    @Operation(
            summary = "Get course creator by UUID",
            description = "Fetches a course creator profile by their unique identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator found",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> getCourseCreatorByUuid(
            @PathVariable UUID uuid) {
        CourseCreatorDTO courseCreatorDTO = courseCreatorService.getCourseCreatorByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(courseCreatorDTO, "Course creator profile fetched successfully"));
    }

    @Operation(
            summary = "Get all course creators",
            description = "Fetches a paginated list of all course creator profiles in the system."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> getAllCourseCreators(
            Pageable pageable) {
        Page<CourseCreatorDTO> courseCreators = courseCreatorService.getAllCourseCreators(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(courseCreators, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course creators fetched successfully"));
    }

    @Operation(
            summary = "Update a course creator",
            description = "Updates an existing course creator profile. Only allows updating mutable fields like bio, professional headline, and website.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator updated successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> updateCourseCreator(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseCreatorDTO courseCreatorDTO) {
        CourseCreatorDTO updatedCourseCreator = courseCreatorService.updateCourseCreator(uuid, courseCreatorDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCourseCreator, "Course creator updated successfully"));
    }

    @Operation(
            summary = "Delete a course creator",
            description = "Removes a course creator profile from the system. This will cascade delete associated data.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Course creator deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteCourseCreator(@PathVariable UUID uuid) {
        courseCreatorService.deleteCourseCreator(uuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Search course creators",
            description = """
                    Search for course creators using flexible criteria with advanced operators.
                   \s
                    **Basic Search:**
                    - `field=value` - Exact match (default operation)
                    - `fullName=John` - Find course creators with fullName exactly "John"
                   \s
                    **Comparison Operators:**
                    - `field_gt=value` - Greater than
                    - `field_lt=value` - Less than \s
                    - `field_gte=value` - Greater than or equal
                    - `field_lte=value` - Less than or equal
                    - `createdDate_gte=2024-01-01T00:00:00` - Created after Jan 1, 2024
                   \s
                    **String Operations:**
                    - `field_like=value` - Contains (case-insensitive)
                    - `field_startswith=value` - Starts with (case-insensitive) \s
                    - `field_endswith=value` - Ends with (case-insensitive)
                    - `fullName_like=alice` - Full name contains "alice"
                   \s
                    **Boolean Operations:**
                    - `adminVerified=true` - Only verified course creators
                    - `adminVerified=false` - Only unverified course creators
                   \s
                    **List Operations:**
                    - `field_in=val1,val2,val3` - Field is in list
                    - `field_notin=val1,val2` - Field is not in list
                   \s
                    **Negation:**
                    - `field_noteq=value` - Not equal to value
                   \s
                    **Examples:**
                    - `/search?fullName_like=john&adminVerified=true`
                    - `/search?createdDate_gte=2024-01-01T00:00:00`
                    - `/search?professionalHeadline_like=content`
                   \s""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned successfully",
                            content = @Content(schema = @Schema(implementation = Page.class)))
            }
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> searchCourseCreators(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorDTO> searchResults = courseCreatorService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(searchResults, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Search completed successfully"));
    }

    // ===== VERIFICATION MANAGEMENT =====

    @Operation(
            summary = "Verify a course creator",
            description = "Marks a course creator as verified by an administrator. Only system admins can perform this operation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator verified successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            }
    )
    @PostMapping("/{uuid}/verify")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> verifyCourseCreator(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String reason) {
        CourseCreatorDTO verifiedCourseCreator = courseCreatorService.verifyCourseCreator(uuid, reason);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(verifiedCourseCreator, "Course creator verified successfully"));
    }

    @Operation(
            summary = "Unverify a course creator",
            description = "Removes verification status from a course creator. Only system admins can perform this operation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator unverified successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            }
    )
    @PostMapping("/{uuid}/unverify")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> unverifyCourseCreator(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String reason) {
        CourseCreatorDTO unverifiedCourseCreator = courseCreatorService.unverifyCourseCreator(uuid, reason);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(unverifiedCourseCreator, "Course creator verification removed successfully"));
    }

    @Operation(
            summary = "Check if course creator is verified",
            description = "Returns the verification status of a course creator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verification status retrieved"),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @GetMapping("/{uuid}/verification-status")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Boolean>> isCourseCreatorVerified(
            @PathVariable UUID uuid) {
        boolean isVerified = courseCreatorService.isCourseCreatorVerified(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(isVerified, "Verification status retrieved successfully"));
    }

    @Operation(
            summary = "Get verified course creators",
            description = "Fetches a paginated list of all verified course creators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verified course creators retrieved successfully")
            }
    )
    @GetMapping("/verified")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> getVerifiedCourseCreators(
            Pageable pageable) {
        Page<CourseCreatorDTO> verifiedCourseCreators = courseCreatorService.getVerifiedCourseCreators(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(verifiedCourseCreators, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Verified course creators fetched successfully"));
    }

    @Operation(
            summary = "Get unverified course creators",
            description = "Fetches a paginated list of all unverified course creators pending admin review.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Unverified course creators retrieved successfully")
            }
    )
    @GetMapping("/unverified")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> getUnverifiedCourseCreators(
            Pageable pageable) {
        Page<CourseCreatorDTO> unverifiedCourseCreators = courseCreatorService.getUnverifiedCourseCreators(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(unverifiedCourseCreators, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Unverified course creators fetched successfully"));
    }

    @Operation(
            summary = "Get course creator count by verification status",
            description = "Returns the total count of course creators filtered by verification status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
            }
    )
    @GetMapping("/count")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Long>> countCourseCreatorsByVerificationStatus(
            @RequestParam boolean verified) {
        long count = courseCreatorService.countCourseCreatorsByVerificationStatus(verified);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(count, "Course creator count retrieved successfully"));
    }
}