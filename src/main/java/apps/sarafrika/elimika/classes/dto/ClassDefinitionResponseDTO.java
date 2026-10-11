package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ClassDefinitionResponse",
        description = "Response payload for class definition operations"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassDefinitionResponseDTO(

        @Schema(description = "Persisted class definition")
        @JsonProperty("class_definition")
        ClassDefinitionDTO classDefinition,

        @Schema(description = "**[READ-ONLY]** On a near-me listing (near=lat,lng) only: how far the class is from the "
                + "searched point, as a coarse band. Never metres.",
                example = "2-5 km", allowableValues = {"<2 km", "2-5 km", "5-10 km", "10-25 km", ">25 km"},
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "distance_band", access = JsonProperty.Access.READ_ONLY)
        String distanceBand,

        @Schema(description = "**[READ-ONLY]** On class listings: the title of the course this class delivers.",
                example = "Introduction to Data Science",
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "course_title", access = JsonProperty.Access.READ_ONLY)
        String courseTitle,

        @Schema(description = "**[READ-ONLY]** On class listings: the title of the training program this class delivers.",
                example = "Full-Stack Developer Pathway",
                accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "program_title", access = JsonProperty.Access.READ_ONLY)
        String programTitle
) {

    public ClassDefinitionResponseDTO(ClassDefinitionDTO classDefinition) {
        this(classDefinition, null, null, null);
    }

    /** A copy carrying the batch-resolved course and program titles. */
    public ClassDefinitionResponseDTO withTitles(String courseTitle, String programTitle) {
        return new ClassDefinitionResponseDTO(classDefinition, distanceBand, courseTitle, programTitle);
    }

    /** A near-me row: the distance band, and the class's coordinates rounded to town level. */
    public ClassDefinitionResponseDTO forNearMe(String band) {
        return new ClassDefinitionResponseDTO(
                classDefinition == null ? null : classDefinition.withPublicCoordinates(), band,
                courseTitle, programTitle);
    }
}
