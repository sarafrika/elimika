package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "ProgramSkillMapping", description = "One graded source of evidence for a programme skill")
public record ProgramSkillMappingDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[REQUIRED]** Course assessment whose grades give the evidence; its course must be in the programme")
        @NotNull
        @JsonProperty("course_assessment_uuid")
        UUID courseAssessmentUuid,

        @Schema(description = "**[OPTIONAL]** Rubric criterion of that assessment; leave empty to use the whole score")
        @JsonProperty("rubric_criteria_uuid")
        UUID rubricCriteriaUuid,

        @Schema(description = "**[REQUIRED]** Share of this assessment's evidence for the skill; shares per assessment total 100", example = "60.00")
        @NotNull
        @DecimalMin(value = "0.01")
        @DecimalMax(value = "100.0")
        @JsonProperty("share_percentage")
        BigDecimal sharePercentage,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Course the assessment belongs to")
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "course_assessment_title", access = JsonProperty.Access.READ_ONLY)
        String courseAssessmentTitle,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Criterion name, or null for a whole score")
        @JsonProperty(value = "rubric_criteria_name", access = JsonProperty.Access.READ_ONLY)
        String rubricCriteriaName
) {
}
