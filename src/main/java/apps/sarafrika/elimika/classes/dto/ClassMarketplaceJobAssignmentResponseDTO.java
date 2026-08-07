package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ClassMarketplaceJobAssignmentResponse",
        description = "Result of assigning an instructor to a marketplace class job. The job moves to "
                + "AWAITING_CLASS and its resource holds stay reserved until the class is created."
)
public record ClassMarketplaceJobAssignmentResponseDTO(

        @JsonProperty("job")
        ClassMarketplaceJobDTO job
) {
}
