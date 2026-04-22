package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ClassMarketplaceJobAssignmentResponse",
        description = "Result of assigning an instructor to a marketplace class job"
)
public record ClassMarketplaceJobAssignmentResponseDTO(

        @JsonProperty("job")
        ClassMarketplaceJobDTO job,

        @JsonProperty("class_definition")
        ClassDefinitionDTO classDefinition
) {
}
