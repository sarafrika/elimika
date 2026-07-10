package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/mpesa/stk-push} on the mpesa-service gateway.
 */
public record StkPushGatewayRequest(
        @JsonProperty("shortcode_uuid") UUID shortcodeUuid,
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("account_reference") String accountReference,
        @JsonProperty("transaction_desc") String transactionDesc
) {
}
