package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A payout as mpesa-service reports it; only the fields elimika uses are mapped. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MpesaPayoutGatewayResponse(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("client_reference") String clientReference,
        @JsonProperty("status") String status,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("recipient_name") String recipientName,
        @JsonProperty("charges_paid") BigDecimal chargesPaid,
        @JsonProperty("result_desc") String resultDesc,
        @JsonProperty("timed_out_at") OffsetDateTime timedOutAt,
        @JsonProperty("processed_at") OffsetDateTime processedAt
) {
}
