package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * A single course recommendation with the reasons it was chosen.
 * <p>
 * {@code reason} is the first entry of {@code reasons}, kept for existing clients. Every item carries at
 * least one reason, and reasons only ever name the viewer's own courses, a category, a skill count or an
 * organisation, never another learner. Clients quote {@code recommendation_id} back on
 * {@code POST /api/v1/discovery/events} when the item is clicked or dismissed.
 *
 * @author Wilfred Njuguna
 * @version 2.0
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

        @Schema(description = "The main reason, as display text (the first of `reasons`)")
        @JsonProperty("reason")
        String reason,

        @Schema(description = "Ranking score; higher is a stronger match. Comparable only within one response")
        @JsonProperty("score")
        double score,

        @Schema(description = "Why the course was recommended, strongest first")
        @JsonProperty("reasons")
        List<RecommendationReasonDTO> reasons,

        @Schema(description = "Identifies this response; quote it back on discovery events")
        @JsonProperty("recommendation_id")
        UUID recommendationId,

        @Schema(description = "Where the list is shown: for_you, next_steps or similar")
        @JsonProperty("surface")
        String surface,

        @Schema(description = "The scoring version that produced the ranking", example = "rules-v2")
        @JsonProperty("model_version")
        String modelVersion

) {
}
