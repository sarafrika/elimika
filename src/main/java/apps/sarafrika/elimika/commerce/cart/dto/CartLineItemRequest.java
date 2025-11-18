package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
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
@Schema(name = "CartLineItemRequest", description = "Line item definition used when creating or updating a cart")
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

    @Schema(
            description = "Optional metadata forwarded to Medusa and persisted with the line item",
            example = "{\"course_uuid\":\"5f5e0f54-59bb-4c77-b21d-6d496dd1b4b2\",\"class_definition_uuid\":\"0f6b8eaa-1f22-4a1b-9a3e-37cf582f58b7\",\"student_uuid\":\"8f4544d3-1741-47ba-aacc-5c9e0fbcd410\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    @JsonProperty("metadata")
    private Map<String, Object> metadata = Map.of();
}
