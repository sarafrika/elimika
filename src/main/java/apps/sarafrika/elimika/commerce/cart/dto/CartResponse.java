package apps.sarafrika.elimika.commerce.cart.dto;

import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
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
 * Public representation of a Medusa cart exposed through the Elimika API.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CartResponse", description = "Cart summary returned to clients consuming the commerce APIs")
public class CartResponse {

    @Schema(description = "Unique Medusa identifier of the cart", example = "cart_01HZX25RFMBW79S99RWQYJXWCM")
    @JsonProperty("id")
    private final String id;

    @Schema(description = "Region identifier the cart is scoped to", example = "reg_01HZX1W8GX9YYB01X54MB2F15C")
    @JsonProperty("region_id")
    private final String regionId;

    @Schema(description = "Associated Medusa customer identifier", example = "cus_01HZX1X6QAQCCYT11S3R6G9KVN")
    @JsonProperty("customer_id")
    private final String customerId;

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
