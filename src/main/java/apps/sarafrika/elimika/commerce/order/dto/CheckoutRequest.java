package apps.sarafrika.elimika.commerce.order.dto;

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
@Schema(name = "CheckoutRequest", description = "Checkout payload that orchestrates Medusa cart completion")
public class CheckoutRequest {

    @Schema(description = "Identifier of the cart being checked out", example = "cart_01HZX25RFMBW79S99RWQYJXWCM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Cart identifier is required")
    private String cartId;

    @Schema(description = "Email address of the purchasing customer", example = "learner@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "Customer email must be valid")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    @Schema(description = "Optional shipping address identifier to attach to the order", example = "addr_01HZX2F2Z6E0Y0T8VJX3W6PC66")
    private String shippingAddressId;

    @Schema(description = "Optional billing address identifier", example = "addr_01HZX2F7GZ92P2X9YBQB0NQ9E3")
    private String billingAddressId;

    @Schema(description = "Payment provider identifier to use for the checkout", example = "manual", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Payment provider identifier is required")
    private String paymentProviderId;
}
