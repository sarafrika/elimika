package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpesaPayoutFloatResponse(
        @JsonProperty("utility_account_available_funds") BigDecimal utilityAccountAvailableFunds,
        @JsonProperty("working_account_available_funds") BigDecimal workingAccountAvailableFunds,
        @JsonProperty("reported_at") LocalDateTime reportedAt
) {
}
