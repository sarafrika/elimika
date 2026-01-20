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
        ClassDefinitionDTO classDefinition
) {
}
