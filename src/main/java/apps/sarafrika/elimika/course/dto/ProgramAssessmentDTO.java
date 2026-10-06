package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "ProgramAssessment", description = "A weighted assessment component of a training program")
public record ProgramAssessmentDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty(value = "program_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID programUuid,

        @NotBlank
        @Size(max = 255)
        @Schema(example = "Practical")
        @JsonProperty("title")
        String title,

        @NotBlank
        @Size(max = 50)
        @Schema(example = "PRACTICAL")
        @JsonProperty("assessment_type")
        String assessmentType,

        @JsonProperty("description")
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        @DecimalMax(value = "100.00")
        @Schema(example = "40")
        @JsonProperty("weight_percentage")
        BigDecimal weightPercentage,

        @JsonProperty("rubric_uuid")
        UUID rubricUuid,

        @JsonProperty("is_required")
        Boolean isRequired,

        @JsonProperty("active")
        Boolean active,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
