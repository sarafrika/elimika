package apps.sarafrika.elimika.commerce.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * High level checkout command issued by the frontend to finalise a purchase.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CheckoutRequest", description = "Checkout payload that orchestrates Medusa cart completion")
public class CheckoutRequest {

    @Schema(description = "Identifier of the cart being checked out", example = "cart_01HZX25RFMBW79S99RWQYJXWCM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Cart identifier is required")
    @JsonProperty("cart_id")
    private String cartId;

    @Schema(description = "Email address of the purchasing customer", example = "learner@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Customer email must be valid")
    @NotBlank(message = "Customer email is required")
    @JsonProperty("customer_email")
    private String customerEmail;

    @Schema(description = "Optional shipping address identifier to attach to the order", example = "addr_01HZX2F2Z6E0Y0T8VJX3W6PC66")
    @JsonProperty("shipping_address_id")
    private String shippingAddressId;

    @Schema(description = "Optional billing address identifier", example = "addr_01HZX2F7GZ92P2X9YBQB0NQ9E3")
    @JsonProperty("billing_address_id")
    private String billingAddressId;

    @Schema(description = "Payment provider identifier to use for the checkout", example = "manual", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Payment provider identifier is required")
    @JsonProperty("payment_provider_id")
    private String paymentProviderId;
}
