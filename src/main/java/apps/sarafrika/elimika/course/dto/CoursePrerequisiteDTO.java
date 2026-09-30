package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * One prior course of a course, as returned by {@code GET /api/v1/courses/{uuid}/prerequisites}.
 */
@Schema(name = "CoursePrerequisite", description = "A prior course that a course requires or recommends")
public record CoursePrerequisiteDTO(
        @Schema(description = "**[READ-ONLY]** Identifier of the prerequisite link.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("uuid") UUID uuid,

        @Schema(description = "**[READ-ONLY]** The course that has the prerequisite. On a live course with a "
                + "pending edit this is the draft course after a PUT.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("course_uuid") UUID courseUuid,

        @Schema(description = "The prior course.")
        @JsonProperty("prerequisite_course_uuid") UUID prerequisiteCourseUuid,

        @Schema(description = "**[READ-ONLY]** Name of the prior course.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("prerequisite_course_name") String prerequisiteCourseName,

        @Schema(description = "true: required before starting; false: recommended only.")
        @JsonProperty("is_mandatory") boolean isMandatory
) {
}
