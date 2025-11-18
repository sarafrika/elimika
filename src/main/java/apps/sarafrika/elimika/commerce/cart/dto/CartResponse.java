package apps.sarafrika.elimika.commerce.cart.dto;

import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
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
 * Public representation of an internal commerce cart exposed through the Elimika API.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CartResponse", description = "Cart summary returned to clients consuming the commerce APIs")
public class CartResponse {

    @Schema(description = "Unique identifier of the cart", example = "2f6d4d1e-5f2a-4b2e-9f8d-0b7c3e9b5c1a")
    @JsonProperty("id")
    private final String id;

    @Schema(description = "Currency code the cart is priced in", example = "USD")
    @JsonProperty("currency_code")
    private final String currencyCode;

    @Schema(description = "Optional region code used for pricing rules", example = "KE")
    @JsonProperty("region_code")
    private final String regionCode;

    @Schema(description = "Cart status", example = "OPEN")
    @JsonProperty("status")
    private final String status;

    @Schema(description = "Subtotal with up to 4 decimal places", example = "1000.0000")
    @JsonProperty("subtotal")
    private final BigDecimal subtotal;

    @Schema(description = "Tax amount with up to 4 decimal places", example = "0.0000")
    @JsonProperty("tax")
    private final BigDecimal tax;

    @Schema(description = "Discount amount with up to 4 decimal places", example = "0.0000")
    @JsonProperty("discount")
    private final BigDecimal discount;

    @Schema(description = "Shipping amount with up to 4 decimal places", example = "0.0000")
    @JsonProperty("shipping")
    private final BigDecimal shipping;

    @Schema(description = "Total with up to 4 decimal places", example = "1000.0000")
    @JsonProperty("total")
    private final BigDecimal total;

    @Schema(description = "Timestamp when the cart was created", example = "2024-07-20T09:45:00Z", format = "date-time")
    @JsonProperty("created_at")
    private final OffsetDateTime createdAt;

    @Schema(description = "Timestamp when the cart was last updated", example = "2024-07-20T10:15:00Z", format = "date-time")
    @JsonProperty("updated_at")
    private final OffsetDateTime updatedAt;

    @ArraySchema(schema = @Schema(implementation = CartItemResponse.class))
    @JsonProperty("items")
    private final List<CartItemResponse> items;
}
