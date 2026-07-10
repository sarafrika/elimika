package apps.sarafrika.elimika.commerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Current payment status of an order.
 */
@Schema(name = "PaymentStatusResponse", description = "Current payment status of an order")
public record PaymentStatusResponse(
        @Schema(description = "Payment status", example = "CAPTURED")
        @JsonProperty("status")
        String status
) {
}
