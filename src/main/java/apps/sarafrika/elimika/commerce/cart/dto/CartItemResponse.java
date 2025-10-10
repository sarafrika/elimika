package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight representation of a cart line item returned to API consumers.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CartItemResponse", description = "A single line item attached to a cart")
public class CartItemResponse {

    @Schema(description = "Unique identifier of the line item", example = "item_01HZX2KJ6ZG5R2Y4B4S4G0QJZY")
    @JsonProperty("id")
    private final String id;

    @Schema(description = "Human friendly name of the product", example = "Advanced Excel Course")
    @JsonProperty("title")
    private final String title;

    @Schema(description = "Quantity of the product variant", example = "1")
    @JsonProperty("quantity")
    private final int quantity;

    @Schema(description = "Medusa variant identifier", example = "variant_01HZX1Y4K8R0HVWZ4Q6CF6M1AP")
    @JsonProperty("variant_id")
    private final String variantId;

    @Schema(description = "Custom metadata captured for the line item")
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
}
