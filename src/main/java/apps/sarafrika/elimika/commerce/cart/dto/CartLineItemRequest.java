package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single line item being added to a cart from the Elimika frontend.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(
        name = "CartLineItemRequest",
        description = "Line item definition used when creating or updating a cart (metadata is system-managed)"
)
public class CartLineItemRequest {

    @Schema(
            description = "Identifier of the internal product variant to add to the cart",
            example = "course-seat-advanced-excel",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Variant identifier is required")
    @JsonProperty("variant_id")
    private String variantId;

    @Schema(
            description = "Quantity of the variant to add to the cart",
            example = "2",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Min(value = 1, message = "Quantity must be at least 1")
    @JsonProperty("quantity")
    private int quantity;
}
