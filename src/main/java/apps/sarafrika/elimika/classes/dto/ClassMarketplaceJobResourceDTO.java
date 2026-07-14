package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(
        name = "ClassMarketplaceJobResource",
        description = "Organisation resource a marketplace job reserves for its sessions while recruitment runs (venue booked exclusively, equipment pools by quantity)"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobResourceDTO(

        @Schema(description = "**[REQUIRED]** Organisation resource to reserve.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("resource_uuid")
        @NotNull(message = "resource_uuid is required")
        UUID resourceUuid,

        @Schema(description = "Units to reserve per session (must be 1 for venues; defaults to 1).", example = "1", nullable = true)
        @JsonProperty("quantity")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
