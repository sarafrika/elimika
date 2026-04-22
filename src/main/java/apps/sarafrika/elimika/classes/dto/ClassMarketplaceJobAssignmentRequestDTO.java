package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(
        name = "ClassMarketplaceJobAssignmentRequest",
        description = "Selects an approved instructor application and creates the actual class"
)
public record ClassMarketplaceJobAssignmentRequestDTO(

        @JsonProperty("application_uuid")
        @NotNull(message = "application_uuid is required")
        UUID applicationUuid
) {
}
