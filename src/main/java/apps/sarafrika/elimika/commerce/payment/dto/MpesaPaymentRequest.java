package apps.sarafrika.elimika.commerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body to charge an order via M-Pesa STK Push.
 */
@Schema(name = "MpesaPaymentRequest", description = "Phone number to prompt for an M-Pesa STK Push payment")
public record MpesaPaymentRequest(
        @Schema(description = "Customer phone number in 254XXXXXXXXX format", example = "254708374149",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("phone_number")
        @NotBlank(message = "phone_number is required")
        String phoneNumber
) {
}
