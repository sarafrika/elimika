package apps.sarafrika.elimika.commerce.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight representation of a cart line item returned to API consumers.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(name = "CartItemResponse", description = "A single line item attached to a cart")
public class CartItemResponse {

    @Schema(description = "Unique identifier of the line item", example = "item_01HZX2KJ6ZG5R2Y4B4S4G0QJZY")
    private final String id;

    @Schema(description = "Human friendly name of the product", example = "Advanced Excel Course")
    private final String title;

    @Schema(description = "Quantity of the product variant", example = "1")
    private final int quantity;

    @Schema(description = "Medusa variant identifier", example = "variant_01HZX1Y4K8R0HVWZ4Q6CF6M1AP")
    private final String variantId;
}
