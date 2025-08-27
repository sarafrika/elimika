package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for criteria creation that may trigger matrix auto-generation
 * <p>
 * This response encapsulates both the created criteria and potentially
 * the complete matrix if auto-generation was triggered.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-27
 */
@Schema(
    name = "CriteriaCreationResponse",
    description = "Response for criteria creation with potential matrix auto-generation",
    example = """
    {
        "criteria": {
            "uuid": "criteria-uuid-123",
            "componentName": "Technical Proficiency",
            "description": "Assessment of technical skills",
            "displayOrder": 1
        },
        "matrix": {
            "rubric": {...},
            "criteria": [...],
            "scoringLevels": [...],
            "matrixCells": {...},
            "matrixStatistics": {...}
        },
        "matrixGenerated": true,
        "message": "Criterion added and matrix auto-generated successfully"
    }
    """
)
public record CriteriaCreationResponse(
    
    @Schema(
        description = "The created rubric criteria",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("criteria")
    RubricCriteriaDTO criteria,
    
    @Schema(
        description = "The complete matrix if auto-generated, null otherwise",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonProperty("matrix")
    RubricMatrixDTO matrix,
    
    @Schema(
        description = "Whether the matrix was auto-generated",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("matrix_generated")
    boolean matrixGenerated,
    
    @Schema(
        description = "Descriptive message about the operation result",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("message")
    String message
    
) {}