package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "CourseAssessmentLineItemRubricEvaluationRow", description = "Selected rubric scoring level for one criterion in a line-item evaluation")
public record CourseAssessmentLineItemRubricEvaluationRowDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty("criteria_uuid")
        @NotNull(message = "Criteria UUID is required")
        UUID criteriaUuid,

        @JsonProperty(value = "criteria_name", access = JsonProperty.Access.READ_ONLY)
        String criteriaName,

        @JsonProperty("scoring_level_uuid")
        @NotNull(message = "Scoring level UUID is required")
        UUID scoringLevelUuid,

        @JsonProperty(value = "scoring_level_name", access = JsonProperty.Access.READ_ONLY)
        String scoringLevelName,

        @JsonProperty(value = "points", access = JsonProperty.Access.READ_ONLY)
        BigDecimal points,

        @JsonProperty("comments")
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        String comments
) {
}
