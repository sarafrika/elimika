package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

/** Body of mpesa-service's POST /api/v1/mpesa/b2c. */
public record MpesaPayoutGatewayRequest(
        @JsonProperty("shortcode_uuid") UUID shortcodeUuid,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("client_reference") String clientReference,
        @JsonProperty("remarks") String remarks
) {
}
