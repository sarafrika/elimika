package apps.sarafrika.elimika.commerce.order.dto;

import apps.sarafrika.elimika.commerce.cart.dto.CartItemResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "OrderResponse", description = "Order information synchronised from Medusa")
public class OrderResponse {

    @Schema(description = "Unique Medusa identifier of the order", example = "order_01HZX2Z2C2MBK8C6Y8PF0YTW30")
    @JsonProperty("id")
    private final String id;

    @Schema(description = "Human friendly order number", example = "100012")
    @JsonProperty("display_id")
    private final String displayId;

    @Schema(description = "Payment status reported by Medusa", example = "captured")
    @JsonProperty("payment_status")
    private final String paymentStatus;

    @Schema(description = "Timestamp when the order was created", example = "2024-07-20T09:55:00Z", format = "date-time")
    @JsonProperty("created_at")
    private final OffsetDateTime createdAt;

    @ArraySchema(schema = @Schema(implementation = CartItemResponse.class))
    @JsonProperty("items")
    private final List<CartItemResponse> items;
}
