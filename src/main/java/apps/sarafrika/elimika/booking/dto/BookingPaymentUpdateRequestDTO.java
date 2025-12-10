package apps.sarafrika.elimika.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(
        name = "BookingPaymentUpdateRequest",
        description = "Callback payload used by payment engine to update booking status"
)
public record BookingPaymentUpdateRequestDTO(

        @Schema(description = "Payment reference provided by the payment engine", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Payment reference is required")
        @JsonProperty("payment_reference")
        String paymentReference,

        @Schema(description = "Payment status reported by the engine", allowableValues = {"succeeded", "failed"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Payment status is required")
        @Pattern(regexp = "^(succeeded|failed)$", message = "Payment status must be 'succeeded' or 'failed'")
        @JsonProperty("payment_status")
        String paymentStatus,

        @Schema(description = "Payment engine identifier", example = "stripe", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("payment_engine")
        String paymentEngine
) {
}
