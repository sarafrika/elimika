package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.RubricScoringLevelDTO;
import apps.sarafrika.elimika.course.service.RubricScoringLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing rubric scoring levels
 * <p>
 * Provides endpoints for managing custom scoring levels within rubrics,
 * enabling flexible matrix-based assessment configurations.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@RestController
@RequestMapping(RubricScoringLevelController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Rubric Scoring Levels", description = "Management of custom scoring levels within assessment rubrics for matrix-based evaluation")
public class RubricScoringLevelController {

    public static final String API_ROOT_PATH = "/api/v1/rubrics/{rubricUuid}/scoring-levels";

    private final RubricScoringLevelService rubricScoringLevelService;

    @Operation(
            summary = "Create a new scoring level for a rubric",
            description = "Creates a new custom scoring level (e.g., Excellent, Good, Fair) within the specified rubric for matrix-based assessment."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringLevelDTO>> createRubricScoringLevel(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Valid @RequestBody RubricScoringLevelDTO rubricScoringLevelDTO) {
        
        RubricScoringLevelDTO createdLevel = rubricScoringLevelService.createRubricScoringLevel(rubricUuid, rubricScoringLevelDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdLevel, "Rubric scoring level created successfully"));
    }

    @Operation(
            summary = "Get all scoring levels for a rubric",
            description = "Retrieves all custom scoring levels for the specified rubric, ordered by level order."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<RubricScoringLevelDTO>>> getScoringLevelsByRubric(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        List<RubricScoringLevelDTO> scoringLevels = rubricScoringLevelService.getScoringLevelsByRubricUuid(rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(scoringLevels, "Scoring levels retrieved successfully"));
    }

    @Operation(
            summary = "Get scoring levels for a rubric with pagination",
            description = "Retrieves custom scoring levels for the specified rubric with pagination support."
    )
    @GetMapping(value = "/paged", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<RubricScoringLevelDTO>>> getScoringLevelsByRubricPaged(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            Pageable pageable) {
        
        Page<RubricScoringLevelDTO> scoringLevelsPage = rubricScoringLevelService.getScoringLevelsByRubricUuid(rubricUuid, pageable);
        PagedDTO<RubricScoringLevelDTO> pagedResponse = new PagedDTO<>(scoringLevelsPage);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Scoring levels retrieved successfully"));
    }

    @Operation(
            summary = "Get a specific scoring level",
            description = "Retrieves a specific scoring level by its UUID within the context of the rubric."
    )
    @GetMapping(value = "/{levelUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringLevelDTO>> getScoringLevel(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "UUID of the scoring level", required = true)
            @PathVariable UUID levelUuid) {
        
        RubricScoringLevelDTO scoringLevel = rubricScoringLevelService.getRubricScoringLevelByUuid(levelUuid);
        return ResponseEntity.ok(ApiResponse.success(scoringLevel, "Scoring level retrieved successfully"));
    }

    @Operation(
            summary = "Update a scoring level",
            description = "Updates an existing scoring level within the specified rubric."
    )
    @PutMapping(value = "/{levelUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringLevelDTO>> updateScoringLevel(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "UUID of the scoring level", required = true)
            @PathVariable UUID levelUuid,
            @Valid @RequestBody RubricScoringLevelDTO rubricScoringLevelDTO) {
        
        RubricScoringLevelDTO updatedLevel = rubricScoringLevelService.updateRubricScoringLevel(rubricUuid, levelUuid, rubricScoringLevelDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedLevel, "Scoring level updated successfully"));
    }

    @Operation(
            summary = "Delete a scoring level",
            description = "Removes a scoring level from the specified rubric. This will also remove any associated matrix cells."
    )
    @DeleteMapping(value = "/{levelUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> deleteScoringLevel(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "UUID of the scoring level", required = true)
            @PathVariable UUID levelUuid) {
        
        rubricScoringLevelService.deleteRubricScoringLevel(rubricUuid, levelUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Scoring level deleted successfully"));
    }

    @Operation(
            summary = "Get passing scoring levels",
            description = "Retrieves only the scoring levels that are marked as passing for the specified rubric."
    )
    @GetMapping(value = "/passing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<RubricScoringLevelDTO>>> getPassingScoringLevels(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        List<RubricScoringLevelDTO> passingScoringLevels = rubricScoringLevelService.getPassingScoringLevels(rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(passingScoringLevels, "Passing scoring levels retrieved successfully"));
    }

    @Operation(
            summary = "Get highest scoring level",
            description = "Retrieves the highest performance scoring level (level_order = 1) for the specified rubric."
    )
    @GetMapping(value = "/highest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringLevelDTO>> getHighestScoringLevel(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        RubricScoringLevelDTO highestLevel = rubricScoringLevelService.getHighestScoringLevel(rubricUuid);
        if (highestLevel != null) {
            return ResponseEntity.ok(ApiResponse.success(highestLevel, "Highest scoring level retrieved successfully"));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null, "No scoring levels found for this rubric"));
        }
    }

    @Operation(
            summary = "Reorder scoring levels",
            description = "Updates the display order of scoring levels within the rubric. Provide a map of level UUIDs to their new order values."
    )
    @PatchMapping(value = "/reorder", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> reorderScoringLevels(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @RequestBody Map<UUID, Integer> levelOrderMap) {
        
        rubricScoringLevelService.reorderScoringLevels(rubricUuid, levelOrderMap);
        return ResponseEntity.ok(ApiResponse.success(null, "Scoring levels reordered successfully"));
    }

    @Operation(
            summary = "Create default scoring levels",
            description = "Creates a set of default scoring levels for the rubric based on the specified template (standard, simple, advanced)."
    )
    @PostMapping(value = "/defaults", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<RubricScoringLevelDTO>>> createDefaultScoringLevels(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "Template to use: standard, simple, or advanced", required = true)
            @RequestParam String template,
            @Parameter(description = "User creating the levels", required = false)
            @RequestParam(defaultValue = "SYSTEM") String createdBy) {
        
        List<RubricScoringLevelDTO> defaultLevels = rubricScoringLevelService.createDefaultScoringLevels(rubricUuid, template, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(defaultLevels, String.format("Default '%s' scoring levels created successfully", template)));
    }
}