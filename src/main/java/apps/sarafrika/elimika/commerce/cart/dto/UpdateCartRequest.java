package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * Represents partial updates that can be applied to a cart.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "UpdateCartRequest", description = "Fields that can be patched on an existing cart")
public class UpdateCartRequest {

    @Schema(description = "Email address of the customer", example = "learner@example.com")
    @Email(message = "Email address must be valid")
    @JsonProperty("email")
    private String email;

    @Schema(description = "Customer identifier to associate with the cart", example = "user-uuid")
    @JsonProperty("customer_id")
    private String customerId;

    @Schema(description = "Shipping address identifier", example = "address-uuid")
    @JsonProperty("shipping_address_id")
    private String shippingAddressId;

    @Schema(description = "Billing address identifier", example = "address-uuid")
    @JsonProperty("billing_address_id")
    private String billingAddressId;

    public boolean hasUpdates() {
        return StringUtils.hasText(email)
                || StringUtils.hasText(customerId)
                || StringUtils.hasText(shippingAddressId)
                || StringUtils.hasText(billingAddressId);
    }
}
