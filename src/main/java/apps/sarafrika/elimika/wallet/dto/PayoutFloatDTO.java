package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway.PayoutFloat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "PayoutFloat", description = "The payout shortcode's balance as M-Pesa last reported it")
public record PayoutFloatDTO(
        @Schema(description = "The shortcode's Utility account balance")
        @JsonProperty("utility_funds") BigDecimal utilityFunds,
        @Schema(description = "Funds available for B2C disbursements")
        @JsonProperty("working_funds") BigDecimal workingFunds,
        @JsonProperty("reported_at") OffsetDateTime reportedAt
) {

    public static PayoutFloatDTO from(PayoutFloat f) {
        return new PayoutFloatDTO(f.utilityAccountFunds(), f.workingAccountFunds(), f.reportedAt());
    }
}
