package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Hours one learner group spends on one lesson. */
@Schema(name = "LessonHoursRequest", description = "Hours a learner group spends on one lesson of the course or program")
public record LessonHoursRequest(

        @Schema(description = "**[REQUIRED]** An active lesson of the course (for programs, of one of its courses).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("lesson_uuid")
        @NotNull(message = "lesson_uuid is required")
        UUID lessonUuid,

        @Schema(description = "**[REQUIRED]** Hours for this lesson, above zero and at most 24.", example = "0.75",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("hours")
        @NotNull(message = "hours is required")
        @DecimalMin(value = "0", inclusive = false, message = "hours must be greater than zero")
        @DecimalMax(value = "24", message = "hours must be at most 24")
        @Digits(integer = 2, fraction = 2, message = "hours must have at most 2 decimals")
        BigDecimal hours
) {
}
