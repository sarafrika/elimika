package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload used by the Elimika frontend to bootstrap a checkout cart using the internal commerce stack.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CreateCartRequest", description = "Request body for creating a new cart")
public class CreateCartRequest {

    @Schema(
            description = "Currency code the cart is priced in",
            example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Currency code is required")
    @JsonProperty("currency_code")
    private String currencyCode;

    @Schema(
            description = "Optional region code for pricing rules",
            example = "KE",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonProperty("region_code")
    private String regionCode;

    @Valid
    @Builder.Default
    @ArraySchema(
            schema = @Schema(implementation = CartLineItemRequest.class),
            arraySchema = @Schema(
                    description = "Optional collection of line items to pre-populate the cart",
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
    )
    @JsonProperty("items")
    private List<CartLineItemRequest> items = List.of();
}
