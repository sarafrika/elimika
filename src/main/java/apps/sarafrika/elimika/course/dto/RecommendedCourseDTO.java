package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * A single course recommendation for a user, with a short human-readable reason.
 * <p>
 * Prototype heuristic engine — recommendations are derived from the user's past
 * courses (authored and/or approved-to-train) by topic and level overlap, with a
 * popularity fallback when there is no history.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-10
 */
@Schema(name = "RecommendedCourse", description = "A recommended course with an explanation")
public record RecommendedCourseDTO(

        @Schema(description = "UUID of the recommended course")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "Course name")
        @JsonProperty("name")
        String name,

        @Schema(description = "Course description")
        @JsonProperty("description")
        String description,

        @Schema(description = "Course thumbnail URL")
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @Schema(description = "Short explanation of why this course was recommended")
        @JsonProperty("reason")
        String reason,

        @Schema(description = "Internal ranking score (higher is a stronger match)")
        @JsonProperty("score")
        double score

) {
}
