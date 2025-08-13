package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.course.dto.RubricMatrixDTO;
import apps.sarafrika.elimika.course.service.RubricMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing complete rubric matrices
 * <p>
 * Provides endpoints for managing the complete rubric matrix including
 * criteria, scoring levels, matrix cells, and validation operations.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@RestController
@RequestMapping(RubricMatrixController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Rubric Matrix Management", description = "Complete matrix-based rubric management including criteria, scoring levels, and cell intersections")
public class RubricMatrixController {

    public static final String API_ROOT_PATH = "/api/v1/rubrics/{rubricUuid}/matrix";

    private final RubricMatrixService rubricMatrixService;

    @Operation(
            summary = "Get complete rubric matrix",
            description = "Retrieves the complete rubric matrix including all criteria, scoring levels, matrix cells, and statistics."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO>> getRubricMatrix(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        RubricMatrixDTO matrix = rubricMatrixService.getRubricMatrix(rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(matrix, "Rubric matrix retrieved successfully"));
    }

    @Operation(
            summary = "Initialize rubric matrix",
            description = "Initializes a rubric matrix with default scoring levels and structure. Creates default levels if none exist."
    )
    @PostMapping(value = "/initialize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO>> initializeRubricMatrix(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "Template for default scoring levels", required = false)
            @RequestParam(defaultValue = "standard") String template,
            @Parameter(description = "User initializing the matrix", required = false)
            @RequestParam(defaultValue = "SYSTEM") String createdBy) {
        
        RubricMatrixDTO matrix = rubricMatrixService.initializeRubricMatrix(rubricUuid, template, createdBy);
        return ResponseEntity.ok(ApiResponse.success(matrix, "Rubric matrix initialized successfully"));
    }

    @Operation(
            summary = "Update matrix cell",
            description = "Updates the description for a specific matrix cell (criteria-scoring level intersection)."
    )
    @PutMapping(value = "/cells", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO>> updateMatrixCell(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid,
            @RequestBody Map<String, Object> cellUpdate) {
        
        UUID criteriaUuid = UUID.fromString((String) cellUpdate.get("criteriaUuid"));
        UUID scoringLevelUuid = UUID.fromString((String) cellUpdate.get("scoringLevelUuid"));
        String description = (String) cellUpdate.get("description");
        
        RubricMatrixDTO matrix = rubricMatrixService.updateMatrixCell(rubricUuid, criteriaUuid, scoringLevelUuid, description);
        return ResponseEntity.ok(ApiResponse.success(matrix, "Matrix cell updated successfully"));
    }

    @Operation(
            summary = "Recalculate rubric scores",
            description = "Recalculates maximum and minimum passing scores based on current matrix configuration and weights."
    )
    @PostMapping(value = "/recalculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO>> recalculateScores(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        RubricMatrixDTO matrix = rubricMatrixService.recalculateScores(rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(matrix, "Rubric scores recalculated successfully"));
    }

    @Operation(
            summary = "Validate rubric matrix",
            description = "Validates the rubric matrix for completeness, weight consistency, and readiness for use."
    )
    @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixService.MatrixValidationResult>> validateMatrix(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        RubricMatrixService.MatrixValidationResult validation = rubricMatrixService.validateMatrix(rubricUuid);
        
        String successMessage = validation.isValid() ? 
                "Matrix validation passed" : 
                "Matrix validation completed with issues";
        
        return ResponseEntity.ok(ApiResponse.success(validation, successMessage));
    }

    @Operation(
            summary = "Get matrix statistics",
            description = "Retrieves statistical information about the matrix including completion percentage and score calculations."
    )
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RubricMatrixDTO.MatrixStatisticsDTO>> getMatrixStatistics(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        RubricMatrixDTO matrix = rubricMatrixService.getRubricMatrix(rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(matrix.matrixStatistics(), "Matrix statistics retrieved successfully"));
    }

    @Operation(
            summary = "Check matrix readiness",
            description = "Quick check to determine if the rubric matrix is ready for use in assessments."
    )
    @GetMapping(value = "/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkMatrixReadiness(
            @Parameter(description = "UUID of the rubric", required = true)
            @PathVariable UUID rubricUuid) {
        
        RubricMatrixService.MatrixValidationResult validation = rubricMatrixService.validateMatrix(rubricUuid);
        
        Map<String, Object> readinessInfo = Map.of(
                "isReady", validation.isValid(),
                "completionPercentage", validation.completionPercentage(),
                "weightsValid", validation.weightsValid(),
                "scoresCalculated", validation.scoresCalculated(),
                "message", validation.message()
        );
        
        String message = validation.isValid() ? 
                "Matrix is ready for use" : 
                "Matrix requires attention before use";
        
        return ResponseEntity.ok(ApiResponse.success(readinessInfo, message));
    }
}