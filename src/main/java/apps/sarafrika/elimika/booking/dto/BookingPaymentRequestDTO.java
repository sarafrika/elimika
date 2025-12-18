package apps.sarafrika.elimika.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(
        name = "BookingPaymentRequest",
        description = "Request payload used to initiate a booking payment session"
)
public record BookingPaymentRequestDTO(

        @Schema(description = "Payment engine identifier", example = "stripe", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 64, message = "Payment engine must not exceed 64 characters")
        @JsonProperty("payment_engine")
        String paymentEngine
) {
}
