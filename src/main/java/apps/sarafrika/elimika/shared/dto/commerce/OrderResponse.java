package apps.sarafrika.elimika.shared.dto.commerce;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * API response describing an order stored in the internal commerce stack.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "OrderResponse", description = "Order information synchronised from Medusa")
public class OrderResponse {

    @Schema(description = "Unique identifier of the order", example = "b6d7ab1b-7a21-4b3f-9e52-d6c2f5b2a9f0")
    @JsonProperty("id")
    private final String id;

    @Schema(description = "Human friendly order number", example = "100012")
    @JsonProperty("display_id")
    private final String displayId;

    @Schema(description = "Payment status", example = "CAPTURED")
    @JsonProperty("payment_status")
    private final String paymentStatus;

    @Schema(description = "Currency code associated to the order totals", example = "KES")
    @JsonProperty("currency_code")
    private final String currencyCode;

    @Schema(description = "Subtotal with up to 4 decimal places", example = "1000.0000")
    @JsonProperty("subtotal")
    private final BigDecimal subtotal;

    @Schema(description = "Total with up to 4 decimal places", example = "1030.0000")
    @JsonProperty("total")
    private final BigDecimal total;

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
