package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * Rubric Matrix Data Transfer Object
 * <p>
 * Represents the complete rubric matrix structure including criteria (rows), 
 * scoring levels (columns), and the intersection cells with descriptions.
 * This provides a comprehensive view for matrix-based rubric management.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Schema(
        name = "RubricMatrix",
        description = "Complete rubric matrix with criteria, scoring levels, and intersection descriptions",
        example = """
        {
            "rubric": {
                "uuid": "a1s2s3r4-5u6b-7r8i-9c10-abcdefghijkl",
                "title": "Music Performance Assessment Rubric",
                "uses_custom_levels": true,
                "is_weighted": true,
                "total_weight": 100.00
            },
            "scoring_levels": [
                {
                    "uuid": "rsl1-uuid",
                    "name": "Excellent",
                    "points": 4.00,
                    "level_order": 1,
                    "color_code": "#4CAF50",
                    "is_passing": true
                }
            ],
            "criteria": [
                {
                    "uuid": "rc1-uuid",
                    "component_name": "Technical Proficiency",
                    "weight": 30.00,
                    "display_order": 1
                }
            ],
            "matrix_cells": {
                "rc1-uuid_rsl1-uuid": {
                    "criteria_uuid": "rc1-uuid",
                    "scoring_level_uuid": "rsl1-uuid",
                    "description": "Demonstrates exceptional technical skill with flawless execution",
                    "points": 4.00
                }
            },
            "matrix_statistics": {
                "total_cells": 15,
                "completed_cells": 12,
                "completion_percentage": 80.0,
                "max_possible_score": 400.00,
                "weighted_max_score": 100.00
            }
        }
        """
)
public record RubricMatrixDTO(

        @Schema(
                description = "**[REQUIRED]** The rubric metadata and configuration.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Rubric information is required")
        @JsonProperty("rubric")
        AssessmentRubricDTO rubric,

        @Schema(
                description = "**[REQUIRED]** List of scoring levels (columns) ordered by level_order.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Scoring levels are required")
        @JsonProperty("scoring_levels")
        List<RubricScoringLevelDTO> scoringLevels,

        @Schema(
                description = "**[REQUIRED]** List of criteria (rows) ordered by display_order.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Criteria are required")
        @JsonProperty("criteria")
        List<RubricCriteriaDTO> criteria,

        @Schema(
                description = "**[REQUIRED]** Matrix cells mapping criteria to scoring levels with descriptions. Key format: 'criteriaUuid_scoringLevelUuid'.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Matrix cells are required")
        @JsonProperty("matrix_cells")
        Map<String, RubricMatrixCellDTO> matrixCells,

        @Schema(
                description = "**[READ-ONLY]** Statistical information about the matrix completion and scoring.",
                accessMode = Schema.AccessMode.READ_ONLY,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty(value = "matrix_statistics", access = JsonProperty.Access.READ_ONLY)
        MatrixStatisticsDTO matrixStatistics

) {

    /**
     * Matrix Cell Data Transfer Object
     * <p>
     * Represents a single cell in the rubric matrix where a criteria intersects with a scoring level.
     */
    @Schema(
            name = "RubricMatrixCell",
            description = "Single cell in the rubric matrix representing criteria-scoring level intersection"
    )
    public record RubricMatrixCellDTO(
            
            @Schema(description = "UUID of the criteria (row)")
            @JsonProperty("criteria_uuid")
            UUID criteriaUuid,
            
            @Schema(description = "UUID of the scoring level (column)")
            @JsonProperty("scoring_level_uuid")
            UUID scoringLevelUuid,
            
            @Schema(description = "Description of performance at this intersection")
            @JsonProperty("description")
            String description,
            
            @Schema(description = "Point value for this cell (from scoring level)")
            @JsonProperty("points")
            BigDecimal points,
            
            @Schema(description = "Weighted points considering criteria weight")
            @JsonProperty("weighted_points")
            BigDecimal weightedPoints,
            
            @Schema(description = "Whether this cell is completed/has description")
            @JsonProperty("is_completed")
            boolean isCompleted
    ) {}

    /**
     * Matrix Statistics Data Transfer Object
     * <p>
     * Provides statistical information about the rubric matrix completion and scoring potential.
     */
    @Schema(
            name = "MatrixStatistics",
            description = "Statistical information about rubric matrix completion and scoring"
    )
    public record MatrixStatisticsDTO(
            
            @Schema(description = "Total number of matrix cells (criteria × scoring levels)")
            @JsonProperty("total_cells")
            int totalCells,
            
            @Schema(description = "Number of cells with descriptions")
            @JsonProperty("completed_cells")
            int completedCells,
            
            @Schema(description = "Percentage of cells that are completed")
            @JsonProperty("completion_percentage")
            double completionPercentage,
            
            @Schema(description = "Maximum possible raw score (highest level × criteria count)")
            @JsonProperty("max_possible_score")
            BigDecimal maxPossibleScore,
            
            @Schema(description = "Maximum weighted score considering criteria weights")
            @JsonProperty("weighted_max_score")
            BigDecimal weightedMaxScore,
            
            @Schema(description = "Minimum passing score based on passing levels")
            @JsonProperty("min_passing_score")
            BigDecimal minPassingScore,
            
            @Schema(description = "Whether the matrix is ready for use (high completion rate)")
            @JsonProperty("is_ready_for_use")
            boolean isReadyForUse
    ) {}

    /**
     * Gets a matrix cell by criteria and scoring level UUIDs.
     *
     * @param criteriaUuid UUID of the criteria
     * @param scoringLevelUuid UUID of the scoring level
     * @return Matrix cell or null if not found
     */
    public RubricMatrixCellDTO getMatrixCell(UUID criteriaUuid, UUID scoringLevelUuid) {
        String key = criteriaUuid + "_" + scoringLevelUuid;
        return matrixCells.get(key);
    }

    /**
     * Checks if the matrix is complete (all cells have descriptions).
     *
     * @return true if all matrix cells are completed
     */
    @JsonProperty(value = "is_complete", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Whether all matrix cells have been completed with descriptions.",
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public boolean isComplete() {
        return matrixStatistics != null && 
               matrixStatistics.completedCells() == matrixStatistics.totalCells();
    }

    /**
     * Gets the expected number of matrix cells based on criteria and scoring levels.
     *
     * @return Expected total number of cells in the matrix
     */
    @JsonProperty(value = "expected_cell_count", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Expected number of matrix cells (criteria count × scoring levels count).",
            example = "20",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public int getExpectedCellCount() {
        if (criteria == null || scoringLevels == null) {
            return 0;
        }
        return criteria.size() * scoringLevels.size();
    }
}