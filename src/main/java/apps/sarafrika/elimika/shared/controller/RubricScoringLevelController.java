package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
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
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
@Tag(name = "Rubric Scoring Levels", description = "Management of custom scoring levels within rubrics for comprehensive matrix-based assessment and evaluation")
public class RubricScoringLevelController {

    public static final String API_ROOT_PATH = "/api/v1/rubrics/{rubricUuid}/scoring-levels";

    /** Writes are restricted to the rubric's author and platform admins. */
    static final String WRITE_ACCESS = "@domainSecurityService.isPlatformAdmin()"
            + " or @courseSecurityService.isRubricOwner(#rubricUuid)";

    private final RubricScoringLevelService rubricScoringLevelService;

    @Operation(
            summary = "Create a new scoring level for a rubric",
            description = "Creates a new custom scoring level (e.g., Excellent, Good, Fair) within the specified rubric for matrix-based assessment."
    )
    @PreAuthorize(WRITE_ACCESS)
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
            summary = "Create multiple scoring levels for a rubric (batch)",
            description = "Creates multiple custom scoring levels at once for efficient rubric setup."
    )
    @PreAuthorize(WRITE_ACCESS)
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<RubricScoringLevelDTO>>> createRubricScoringLevelsBatch(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Valid @RequestBody List<RubricScoringLevelDTO> rubricScoringLevelDTOs) {
        
        List<RubricScoringLevelDTO> createdLevels = rubricScoringLevelService.createRubricScoringLevelsBatch(rubricUuid, rubricScoringLevelDTOs);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdLevels, "Rubric scoring levels created successfully"));
    }

    @Operation(
            summary = "Add the standard five scoring levels to a rubric",
            description = "Distinction (5), Merit (4), Pass (3), Fail (2) and No Effort (1); the first three count as passing."
    )
    @PreAuthorize(WRITE_ACCESS)
    @PostMapping(value = "/standard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<RubricScoringLevelDTO>>> createStandardRubricScoringLevels(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        List<RubricScoringLevelDTO> levels = List.of(
                standardLevel(rubricUuid, "Distinction", 5, 1, "#2E7D32", true),
                standardLevel(rubricUuid, "Merit", 4, 2, "#558B2F", true),
                standardLevel(rubricUuid, "Pass", 3, 3, "#F9A825", true),
                standardLevel(rubricUuid, "Fail", 2, 4, "#EF6C00", false),
                standardLevel(rubricUuid, "No Effort", 1, 5, "#C62828", false));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                rubricScoringLevelService.createRubricScoringLevelsBatch(rubricUuid, levels),
                "Standard scoring levels created successfully"));
    }

    private static RubricScoringLevelDTO standardLevel(UUID rubricUuid, String name, int points, int order,
                                                       String colour, boolean passing) {
        return new RubricScoringLevelDTO(null, rubricUuid, name, null, java.math.BigDecimal.valueOf(points), order,
                colour, passing, null, null, null, null);
    }

    @Operation(
            summary = "Get all scoring levels for a rubric",
            description = "Retrieves all custom scoring levels for the specified rubric, ordered by level order."
    )
    @PreAuthorize(AssessmentRubricController.READ_ACCESS_BY_RUBRIC_UUID)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<RubricScoringLevelDTO>>> getScoringLevelsByRubric(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            Pageable pageable) {
        
        Page<RubricScoringLevelDTO> scoringLevelsPage = rubricScoringLevelService.getScoringLevelsByRubricUuidOrderByLevelOrder(rubricUuid, pageable);
        PagedDTO<RubricScoringLevelDTO> pagedResponse = PagedDTO.from(scoringLevelsPage, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString());
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Scoring levels retrieved successfully"));
    }


    @Operation(
            summary = "Get a specific scoring level",
            description = "Retrieves a specific scoring level by its UUID within the context of the rubric."
    )
    @PreAuthorize(AssessmentRubricController.READ_ACCESS_BY_RUBRIC_UUID)
    @GetMapping(value = "/{levelUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricScoringLevelDTO>> getScoringLevel(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "UUID of the scoring level", required = true)
            @PathVariable UUID levelUuid) {
        
        RubricScoringLevelDTO scoringLevel = rubricScoringLevelService.getRubricScoringLevelByUuid(levelUuid);
        // Read access was granted on the path rubric, so the level must belong to it.
        if (!rubricUuid.equals(scoringLevel.rubricUuid())) {
            throw new ResourceNotFoundException(String.format(
                    "Scoring level %s not found in rubric %s", levelUuid, rubricUuid));
        }
        return ResponseEntity.ok(ApiResponse.success(scoringLevel, "Scoring level retrieved successfully"));
    }

    @Operation(
            summary = "Update a scoring level",
            description = "Updates an existing scoring level within the specified rubric."
    )
    @PreAuthorize(WRITE_ACCESS)
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
    @PreAuthorize(WRITE_ACCESS)
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
    @PreAuthorize(AssessmentRubricController.READ_ACCESS_BY_RUBRIC_UUID)
    @GetMapping(value = "/passing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<RubricScoringLevelDTO>>> getPassingScoringLevels(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            Pageable pageable) {
        
        Page<RubricScoringLevelDTO> passingScoringLevelsPage = rubricScoringLevelService.getPassingScoringLevels(rubricUuid, pageable);
        PagedDTO<RubricScoringLevelDTO> pagedResponse = PagedDTO.from(passingScoringLevelsPage, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString());
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Passing scoring levels retrieved successfully"));
    }

    @Operation(
            summary = "Get highest scoring level",
            description = "Retrieves the highest performance scoring level (level_order = 1) for the specified rubric."
    )
    @PreAuthorize(AssessmentRubricController.READ_ACCESS_BY_RUBRIC_UUID)
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
    @PreAuthorize(WRITE_ACCESS)
    @PatchMapping(value = "/reorder", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> reorderScoringLevels(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @RequestBody Map<UUID, Integer> levelOrderMap) {
        
        rubricScoringLevelService.reorderScoringLevels(rubricUuid, levelOrderMap);
        return ResponseEntity.ok(ApiResponse.success(null, "Scoring levels reordered successfully"));
    }

}