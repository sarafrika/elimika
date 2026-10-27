package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** One programme course's mandatory flag and its share of the programme mark. */
@Schema(name = "ProgramCourseWeight", description = "Mandatory flag and programme-mark weight for one course in a programme")
public record ProgramCourseWeightDTO(
        @Schema(description = "**[REQUIRED]** The programme-course association being weighted.")
        @NotNull
        @JsonProperty("program_course_uuid")
        UUID programCourseUuid,

        @Schema(description = "**[REQUIRED]** Mandatory courses carry weight and must be passed; optional ones never count.")
        @NotNull
        @JsonProperty("is_required")
        Boolean isRequired,

        @Schema(description = "**[OPTIONAL]** Share of the programme mark. Required for mandatory courses; must be empty for optional ones.", example = "25.00")
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        @JsonProperty("weight_percentage")
        BigDecimal weightPercentage
) {
}
