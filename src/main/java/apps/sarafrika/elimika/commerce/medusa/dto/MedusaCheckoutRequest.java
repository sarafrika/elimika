package apps.sarafrika.elimika.commerce.medusa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload containing the information required to complete a Medusa cart checkout.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MedusaCheckoutRequest {

    @NotBlank
    @JsonProperty("cart_id")
    private String cartId;

    @Email
    @NotBlank
    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("shipping_address_id")
    private String shippingAddressId;

    @JsonProperty("billing_address_id")
    private String billingAddressId;

    @NotBlank
    @JsonProperty("payment_provider_id")
    private String paymentProviderId;
}
