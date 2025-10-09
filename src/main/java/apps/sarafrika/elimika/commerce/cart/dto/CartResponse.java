package apps.sarafrika.elimika.commerce.cart.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Public representation of a Medusa cart exposed through the Elimika API.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(name = "CartResponse", description = "Cart summary returned to clients consuming the commerce APIs")
public class CartResponse {

    @Schema(description = "Unique Medusa identifier of the cart", example = "cart_01HZX25RFMBW79S99RWQYJXWCM")
    private final String id;

    @Schema(description = "Region identifier the cart is scoped to", example = "reg_01HZX1W8GX9YYB01X54MB2F15C")
    private final String regionId;

    @Schema(description = "Associated Medusa customer identifier", example = "cus_01HZX1X6QAQCCYT11S3R6G9KVN")
    private final String customerId;

    @Schema(description = "Timestamp when the cart was created", example = "2024-07-20T09:45:00Z", format = "date-time")
    private final OffsetDateTime createdAt;

    @Schema(description = "Timestamp when the cart was last updated", example = "2024-07-20T10:15:00Z", format = "date-time")
    private final OffsetDateTime updatedAt;

    @ArraySchema(schema = @Schema(implementation = CartItemResponse.class))
    private final List<CartItemResponse> items;
}
