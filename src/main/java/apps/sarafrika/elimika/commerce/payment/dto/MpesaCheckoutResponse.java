package apps.sarafrika.elimika.commerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after initiating an M-Pesa STK Push for an order.
 */
@Schema(name = "MpesaCheckoutResponse", description = "Result of initiating an M-Pesa STK Push")
public record MpesaCheckoutResponse(
        @Schema(description = "Checkout request id used to poll payment status", example = "ws_CO_04112017184930742")
        @JsonProperty("checkout_request_id")
        String checkoutRequestId,

        @Schema(description = "Initial payment status", example = "PENDING")
        @JsonProperty("status")
        String status
) {
}
