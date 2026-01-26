package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record RevenueAmountDTO(
        @JsonProperty("currency_code")
        String currencyCode,
        @JsonProperty("amount")
        BigDecimal amount
) {
}
