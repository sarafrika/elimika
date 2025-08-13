package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import apps.sarafrika.elimika.course.service.AssessmentRubricService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for rubric discovery and search functionality
 * <p>
 * Provides endpoints for instructors to discover, search, and browse
 * available rubrics for reuse across multiple courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@RestController
@RequestMapping(RubricDiscoveryController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Rubric Discovery", description = "Discovery and search functionality for finding reusable rubrics")
public class RubricDiscoveryController {

    public static final String API_ROOT_PATH = "/api/v1/rubrics/discovery";

    private final AssessmentRubricService assessmentRubricService;

    @Operation(
            summary = "Browse all public rubrics",
            description = "Retrieves all public rubrics available for reuse across courses, ordered by creation date."
    )
    @GetMapping(value = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getPublicRubrics(Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getPublicRubrics(pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/public");
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Public rubrics retrieved successfully"));
    }

    @Operation(
            summary = "Search public rubrics",
            description = "Searches public rubrics by title, description, and optionally by rubric type."
    )
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> searchPublicRubrics(
            @Parameter(description = "Search term to match in title or description", required = false)
            @RequestParam(required = false) String q,
            @Parameter(description = "Filter by rubric type", required = false)
            @RequestParam(required = false) String type,
            Pageable pageable) {
        
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.searchPublicRubrics(q, type, pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/search");
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Search results retrieved successfully"));
    }

    @Operation(
            summary = "Get popular rubrics",
            description = "Retrieves the most popular public rubrics based on usage across multiple courses."
    )
    @GetMapping(value = "/popular", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getPopularRubrics(Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getPopularRubrics(pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/popular");
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Popular rubrics retrieved successfully"));
    }

    @Operation(
            summary = "Get general rubrics",
            description = "Retrieves general-purpose rubrics that are not tied to any specific course."
    )
    @GetMapping(value = "/general", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getGeneralRubrics(Pageable pageable) {
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getGeneralRubrics(pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/general");
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "General rubrics retrieved successfully"));
    }

    @Operation(
            summary = "Get rubrics by status",
            description = "Retrieves rubrics filtered by their content status (e.g., DRAFT, PUBLISHED, ARCHIVED)."
    )
    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getRubricsByStatus(
            @Parameter(description = "Content status to filter by", required = true)
            @PathVariable ContentStatus status,
            Pageable pageable) {
        
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getRubricsByStatus(status, pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/status/" + status);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, 
                String.format("Rubrics with status '%s' retrieved successfully", status)));
    }

    @Operation(
            summary = "Get instructor's rubrics",
            description = "Retrieves rubrics created by a specific instructor, with option to include private rubrics."
    )
    @GetMapping(value = "/instructor/{instructorUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getInstructorRubrics(
            @Parameter(description = "UUID of the instructor", required = true)
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Include private rubrics (default: false)", required = false)
            @RequestParam(defaultValue = "false") boolean includePrivate,
            Pageable pageable) {
        
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.getInstructorRubrics(instructorUuid, includePrivate, pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/instructor/" + instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Instructor's rubrics retrieved successfully"));
    }

    @Operation(
            summary = "Get rubric usage statistics",
            description = "Retrieves overall statistics about rubric usage, including counts of public rubrics, total rubrics, etc."
    )
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRubricStatistics() {
        Map<String, Long> statistics = assessmentRubricService.getRubricStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "Rubric statistics retrieved successfully"));
    }

    @Operation(
            summary = "Get instructor's rubric statistics",
            description = "Retrieves statistics about a specific instructor's rubrics, including counts by visibility and status."
    )
    @GetMapping(value = "/statistics/instructor/{instructorUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Long>>> getInstructorRubricStatistics(
            @Parameter(description = "UUID of the instructor", required = true)
            @PathVariable UUID instructorUuid) {
        
        Map<String, Long> statistics = assessmentRubricService.getInstructorRubricStatistics(instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(statistics, "Instructor's rubric statistics retrieved successfully"));
    }

    @Operation(
            summary = "Browse rubrics by type",
            description = "Retrieves public rubrics filtered by a specific rubric type."
    )
    @GetMapping(value = "/type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<AssessmentRubricDTO>>> getRubricsByType(
            @Parameter(description = "Rubric type to filter by", required = true)
            @PathVariable String type,
            Pageable pageable) {
        
        Page<AssessmentRubricDTO> rubrics = assessmentRubricService.searchPublicRubrics(null, type, pageable);
        PagedDTO<AssessmentRubricDTO> pagedResponse = PagedDTO.from(rubrics, "/api/v1/rubrics/discovery/type/" + type);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, 
                String.format("Rubrics of type '%s' retrieved successfully", type)));
    }
}