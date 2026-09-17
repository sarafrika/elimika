package apps.sarafrika.elimika.timetabling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "InstructorClassOption", description = "A class the instructor has students in, for filtering the student list")
public record InstructorClassOptionDTO(

        @Schema(description = "The class", format = "uuid")
        @JsonProperty(value = "class_definition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID classDefinitionUuid,

        @Schema(description = "Title of the class", example = "Grade 5 Piano - Term 2")
        @JsonProperty(value = "class_title", access = JsonProperty.Access.READ_ONLY)
        String classTitle
) {
}
