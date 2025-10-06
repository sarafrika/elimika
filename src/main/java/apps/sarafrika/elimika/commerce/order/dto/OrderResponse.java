package apps.sarafrika.elimika.commerce.order.dto;

import apps.sarafrika.elimika.commerce.cart.dto.CartItemResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * API response describing an order stored in Medusa.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(name = "OrderResponse", description = "Order information synchronised from Medusa")
public class OrderResponse {

    @Schema(description = "Unique Medusa identifier of the order", example = "order_01HZX2Z2C2MBK8C6Y8PF0YTW30")
    private final String id;

    @Schema(description = "Human friendly order number", example = "100012")
    private final String displayId;

    @Schema(description = "Overall order status", example = "completed")
    private final String status;

    @Schema(description = "Fulfilment status reported by Medusa", example = "fulfilled")
    private final String fulfillmentStatus;

    @Schema(description = "Payment status reported by Medusa", example = "captured")
    private final String paymentStatus;

    @Schema(description = "Timestamp when the order was created", example = "2024-07-20T09:55:00Z", format = "date-time")
    private final OffsetDateTime createdAt;

    @ArraySchema(schema = @Schema(implementation = CartItemResponse.class))
    private final List<CartItemResponse> items;
}
