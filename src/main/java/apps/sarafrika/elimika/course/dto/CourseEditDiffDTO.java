package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "CourseEditDiff",
        description = """
                What a pending edit would change if approved, so an admin can review the
                decision without comparing two full course payloads by eye.
                """,
        example = """
        {
          "course_uuid": "course-1234-5678-90ab-cdef12345678",
          "draft_course_uuid": "draft-1234-5678-90ab-cdef12345678",
          "field_changes": [
            { "field": "name", "live_value": "Intro to Piano", "draft_value": "Introduction to Piano" },
            { "field": "price", "live_value": "1500.00", "draft_value": "1800.00" }
          ],
          "lessons_added": 1,
          "lessons_removed": 0,
          "lessons_modified": 2
        }
        """
)
public record CourseEditDiffDTO(

        @Schema(description = "**[READ-ONLY]** The live course under review.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        java.util.UUID courseUuid,

        @Schema(description = "**[READ-ONLY]** Draft course holding the proposed content.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "draft_course_uuid", access = JsonProperty.Access.READ_ONLY)
        java.util.UUID draftCourseUuid,

        @Schema(description = "**[READ-ONLY]** Course fields that differ between the live course and the draft.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "field_changes", access = JsonProperty.Access.READ_ONLY)
        List<FieldChange> fieldChanges,

        @Schema(description = "**[READ-ONLY]** Lessons the edit adds.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "lessons_added", access = JsonProperty.Access.READ_ONLY)
        int lessonsAdded,

        @Schema(description = "**[READ-ONLY]** Lessons the edit removes.", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "lessons_removed", access = JsonProperty.Access.READ_ONLY)
        int lessonsRemoved,

        @Schema(description = "**[READ-ONLY]** Lessons the edit changes in place.", example = "2", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "lessons_modified", access = JsonProperty.Access.READ_ONLY)
        int lessonsModified
) {

    @Schema(name = "CourseEditFieldChange", description = "A single course field the edit would change.")
    public record FieldChange(

            @Schema(description = "**[READ-ONLY]** Field name, in snake_case as it appears on the course payload.", example = "price")
            @JsonProperty(value = "field", access = JsonProperty.Access.READ_ONLY)
            String field,

            @Schema(description = "**[READ-ONLY]** Current value on the live course, rendered as text.", example = "1500.00")
            @JsonProperty(value = "live_value", access = JsonProperty.Access.READ_ONLY)
            String liveValue,

            @Schema(description = "**[READ-ONLY]** Proposed value on the draft, rendered as text.", example = "1800.00")
            @JsonProperty(value = "draft_value", access = JsonProperty.Access.READ_ONLY)
            String draftValue
    ) {
    }
}
