package apps.sarafrika.elimika.shared.dto.commerce;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API response describing an order stored in Medusa.
 */
@Getter
@Builder(toBuilder = true)
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

    @Schema(description = "Currency code associated to the order totals", example = "KES")
    @JsonProperty("currency_code")
    private final String currencyCode;

    @Schema(description = "Subtotal in the smallest currency denomination", example = "100000")
    @JsonProperty("subtotal")
    private final Long subtotal;

    @Schema(description = "Total in the smallest currency denomination", example = "103000")
    @JsonProperty("total")
    private final Long total;

    @Schema(description = "Timestamp when the order was created", example = "2024-07-20T09:55:00Z", format = "date-time")
    @JsonProperty("created_at")
    private final OffsetDateTime createdAt;

    @Schema(description = "Platform fee breakdown applied to this order")
    @JsonProperty("platform_fee")
    private final PlatformFeeBreakdown platformFee;

    @ArraySchema(schema = @Schema(implementation = CartItemResponse.class))
    @JsonProperty("items")
    private final List<CartItemResponse> items;
}
