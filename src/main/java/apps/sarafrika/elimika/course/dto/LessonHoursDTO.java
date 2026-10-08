package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** One lesson of a learner group's plan; the title is null when the lesson has since been removed. */
@Schema(name = "LessonHours", description = "Hours a learner group spends on one lesson")
public record LessonHoursDTO(
        @JsonProperty("lesson_uuid") UUID lessonUuid,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("lesson_title") String lessonTitle,
        @JsonProperty("lesson_number") Integer lessonNumber,
        @JsonProperty("hours") BigDecimal hours
) {
}
