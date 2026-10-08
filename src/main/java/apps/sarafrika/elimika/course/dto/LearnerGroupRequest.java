package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** A named age band an instructor would teach, with hours for every lesson. */
@Schema(name = "LearnerGroupRequest", description = "An instructor's learner group: a named age band with its own lesson plan")
public record LearnerGroupRequest(

        @Schema(description = "**[REQUIRED]** Group name, unique within the application.", example = "Juniors", maxLength = 80,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("name")
        @NotBlank(message = "Learner group name is required")
        @Size(max = 80, message = "Learner group name must not exceed 80 characters")
        String name,

        @Schema(description = "**[REQUIRED]** Youngest age in the group, within the course's age range.", example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("min_age")
        @NotNull(message = "min_age is required")
        @Min(value = 0, message = "min_age must be 0 or more")
        @Max(value = 120, message = "min_age must be at most 120")
        Integer minAge,

        @Schema(description = "**[REQUIRED]** Oldest age in the group, within the course's age range.", example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("max_age")
        @NotNull(message = "max_age is required")
        @Min(value = 0, message = "max_age must be 0 or more")
        @Max(value = 120, message = "max_age must be at most 120")
        Integer maxAge,

        @Schema(description = "**[REQUIRED]** Hours for every active lesson of the course (for programs, of all its courses).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("lesson_hours")
        @NotNull(message = "lesson_hours is required")
        @Valid
        List<LessonHoursRequest> lessonHours
) {
}
