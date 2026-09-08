package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Course content scoped to whoever asked for it.
 * <p>
 * {@code access} names the footing the caller is on — creator, admin, organisation, instructor,
 * student, pending, applicant or prospect — and {@code full_access} says whether that footing
 * carries the right to read lesson bodies. When it does not, the lessons carry an outline and
 * nothing else: no content items, no lesson identifiers. The summary is still enough to decide
 * whether to apply or enrol, which is its whole purpose.
 * <p>
 * Content is never editable here whatever the access; only the course creator may edit it.
 */
@Schema(name = "OrganisationCourseContent", description = "Course content scoped to the caller. Outline only unless the caller's access carries full read rights.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganisationCourseContentDTO(

        @Schema(description = "The course this content belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "The footing the caller views this course on. Resolved server-side; never re-derived by the client.", example = "prospect")
        @JsonProperty("access")
        CourseContentAccess access,

        @Schema(description = "True when the caller's access carries full read rights and lesson content is therefore included.", example = "false")
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

        @Schema(description = "Lessons. Outline only without full access, then with full content.")
        @JsonProperty("lessons")
        List<OrganisationCourseLessonDTO> lessons,

        @Schema(description = "The course itself: title, blurb, objectives, prerequisites and what a "
                + "trainer must supply. Present for every caller, so a course page can be rendered "
                + "without a second, authenticated request. Carries no commercial terms.")
        @JsonProperty("course")
        PublicCourseProfileDTO course
) {
}
