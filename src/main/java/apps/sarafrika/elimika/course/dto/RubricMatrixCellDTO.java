package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Matrix Cell Data Transfer Object
 * <p>
 * Represents a single cell in the rubric matrix where a criteria intersects with a scoring level.
 * Contains both the intersection coordinates and calculated values for display purposes.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-28
 */
@Schema(
        name = "RubricMatrixCell",
        description = "Single cell in the rubric matrix representing criteria-scoring level intersection",
        example = """
        {
            "criteria_uuid": "rc1-uuid",
            "scoring_level_uuid": "rsl1-uuid", 
            "description": "Demonstrates exceptional technical skill with flawless execution",
            "points": 4.00
        }
        """
)
public record RubricMatrixCellDTO(
        
        @Schema(
                description = "**[REQUIRED]** UUID of the criteria (row) this cell belongs to.",
                example = "rc1-uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Criteria UUID is required")
        @JsonProperty("criteria_uuid")
        UUID criteriaUuid,
        
        @Schema(
                description = "**[REQUIRED]** UUID of the scoring level (column) this cell belongs to.",
                example = "rsl1-uuid",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Scoring level UUID is required")
        @JsonProperty("scoring_level_uuid")
        UUID scoringLevelUuid,
        
        @Schema(
                description = "**[REQUIRED]** Description of performance expectations at this intersection.",
                example = "Demonstrates exceptional technical skill with flawless execution",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 1000
        )
        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        @JsonProperty("description")
        String description,
        
        @Schema(
                description = "**[READ-ONLY]** Point value for this cell (derived from scoring level).",
                example = "4.00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "points", access = JsonProperty.Access.READ_ONLY)
        BigDecimal points
        
) {
}