package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * The full prerequisite set for a course, replacing whatever it had. An empty list clears it.
 */
@Schema(name = "CoursePrerequisitesRequest", description = "Replaces a course's prerequisite set",
        example = """
                {
                  "prerequisites": [
                    {"prerequisite_course_uuid": "0f8fad5b-d9cb-469f-a165-70867728950e", "is_mandatory": true},
                    {"prerequisite_course_uuid": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "is_mandatory": false}
                  ]
                }
                """)
public record CoursePrerequisitesRequest(
        @Schema(description = "**[REQUIRED]** Every prior course the course should have; an empty list clears them.")
        @NotNull(message = "prerequisites is required; send an empty list to clear them")
        @Size(max = 50, message = "A course can have at most 50 prerequisites")
        @Valid
        @JsonProperty("prerequisites") List<Item> prerequisites
) {

    @Schema(name = "CoursePrerequisitesRequestItem")
    public record Item(
            @Schema(description = "**[REQUIRED]** A published course, or one of the author's own courses.")
            @NotNull(message = "prerequisite_course_uuid is required")
            @JsonProperty("prerequisite_course_uuid") UUID prerequisiteCourseUuid,

            @Schema(description = "**[OPTIONAL]** true (default): required; false: recommended only.")
            @JsonProperty("is_mandatory") Boolean isMandatory
    ) {
        public boolean mandatory() {
            return isMandatory == null || isMandatory;
        }
    }
}
