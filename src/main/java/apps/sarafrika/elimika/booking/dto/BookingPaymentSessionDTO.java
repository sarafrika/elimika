package apps.sarafrika.elimika.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "BookingPaymentSession",
        description = "Payment session details for a booking"
)
public record BookingPaymentSessionDTO(

        @Schema(description = "Booking UUID", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("booking_uuid")
        UUID bookingUuid,

        @Schema(description = "Payment session identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("payment_session_id")
        String paymentSessionId,

        @Schema(description = "Payment URL to complete the booking payment")
        @JsonProperty("payment_url")
        String paymentUrl,

        @Schema(description = "Payment engine used for this booking")
        @JsonProperty("payment_engine")
        String paymentEngine,

        @Schema(description = "When the hold on the slot expires if unpaid")
        @JsonProperty("hold_expires_at")
        LocalDateTime holdExpiresAt
) {
}
