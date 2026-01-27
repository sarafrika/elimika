package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RevenuePaymentDTO(
        @JsonProperty("payment_uuid")
        UUID paymentUuid,
        @JsonProperty("order_uuid")
        UUID orderUuid,
        @JsonProperty("order_total_amount")
        BigDecimal orderTotalAmount,
        @JsonProperty("order_currency_code")
        String orderCurrencyCode,
        @JsonProperty("provider")
        String provider,
        @JsonProperty("status")
        String status,
        @JsonProperty("amount")
        BigDecimal amount,
        @JsonProperty("currency_code")
        String currencyCode,
        @JsonProperty("external_reference")
        String externalReference,
        @JsonProperty("processed_at")
        LocalDateTime processedAt
) {
}
