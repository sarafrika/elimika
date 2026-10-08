package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** An age group an instructor or organisation keeps for reuse; applications copy it. */
@Schema(name = "SavedAgeGroupRequest", description = "A reusable age group: a named age band with no lesson plan")
public record SavedAgeGroupRequest(

        @Schema(description = "**[REQUIRED]** Name, unique for its owner.", example = "Juniors", maxLength = 80,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("name")
        @NotBlank(message = "Age group name is required")
        @Size(max = 80, message = "Age group name must not exceed 80 characters")
        String name,

        @Schema(description = "**[REQUIRED]** Youngest age.", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("min_age")
        @NotNull(message = "min_age is required")
        @Min(value = 0, message = "min_age must be 0 or more")
        @Max(value = 120, message = "min_age must be at most 120")
        Integer minAge,

        @Schema(description = "**[REQUIRED]** Oldest age.", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("max_age")
        @NotNull(message = "max_age is required")
        @Min(value = 0, message = "max_age must be 0 or more")
        @Max(value = 120, message = "max_age must be at most 120")
        Integer maxAge
) {
}
