package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Course content served to an organisation, gated by whether that organisation has an
 * approved application to train the course.
 * <p>
 * Not approved: a summary the school can use to decide whether to apply — lesson outline,
 * content counts and rating, but no lesson bodies. Approved: full read access to every
 * lesson's content. Content is never editable here; only the course creator can edit it.
 */
@Schema(name = "OrganisationCourseContent", description = "Approval-gated course content for an organisation. Summary when not approved, full content when approved.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganisationCourseContentDTO(

        @Schema(description = "The course this content belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "True when the organisation is approved to train and therefore has full read access.", example = "false")
        @JsonProperty("full_access")
        boolean fullAccess,

        @Schema(description = "Total number of lessons in the course.", example = "12")
        @JsonProperty("total_lessons")
        int totalLessons,

        @Schema(description = "Average learner rating (1-5), or null when there are no reviews.", example = "4.6")
        @JsonProperty("average_rating")
        Double averageRating,

        @Schema(description = "Number of learner reviews.", example = "37")
        @JsonProperty("total_reviews")
        int totalReviews,

        @Schema(description = "Lessons. Outline only until approved, then with full content.")
        @JsonProperty("lessons")
        List<OrganisationCourseLessonDTO> lessons
) {
}
