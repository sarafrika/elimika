package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment read-model returned by {@code GET /api/v1/mpesa/payments/by-checkout/{checkoutRequestId}}.
 * The {@code status} is one of PENDING/SUCCESS/FAILED/CANCELLED.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MpesaPaymentStatusResponse(
        @JsonProperty("status") String status
) {
}
