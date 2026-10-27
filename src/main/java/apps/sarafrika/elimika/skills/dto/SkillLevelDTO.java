package apps.sarafrika.elimika.skills.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "SkillLevel", description = "One level of the platform-wide skill scale")
public record SkillLevelDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Name learners see, e.g. Developing", example = "Achieved")
        @NotBlank
        @Size(max = 60)
        @JsonProperty("name")
        String name,

        @Schema(description = "Lowest evidence score that earns this level", example = "70.00")
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @JsonProperty("min_percentage")
        BigDecimal minPercentage,

        @Schema(description = "Achieved levels sync to the learner's skills wallet; they must be the top levels")
        @NotNull
        @JsonProperty("counts_as_achieved")
        Boolean countsAsAchieved
) {
}
