package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A stored learner group with its lesson plan, in the order the applicant listed them. */
@Schema(name = "LearnerGroup", description = "An instructor's learner group: a named age band with its own lesson plan")
public record LearnerGroupDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("name") String name,
        @JsonProperty("min_age") Integer minAge,
        @JsonProperty("max_age") Integer maxAge,
        @Schema(description = "Sum of the group's lesson hours.")
        @JsonProperty("total_hours") BigDecimal totalHours,
        @JsonProperty("lesson_hours") List<LessonHoursDTO> lessonHours
) {
}
