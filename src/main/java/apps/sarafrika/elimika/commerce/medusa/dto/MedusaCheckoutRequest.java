package apps.sarafrika.elimika.commerce.medusa.dto;

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
    private String cartId;

    @Email
    @NotBlank
    private String customerEmail;

    private String shippingAddressId;

    private String billingAddressId;

    @NotBlank
    private String paymentProviderId;
}
