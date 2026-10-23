package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.entity.PayoutSettings;
import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "PayoutRules", description = "What an earner needs to know before withdrawing")
public record PayoutRulesDTO(
        @JsonProperty("min_amount") BigDecimal minAmount,
        @JsonProperty("max_amount") BigDecimal maxAmount,
        @JsonProperty("daily_limit") BigDecimal dailyLimit,
        @JsonProperty("fee_payer") PayoutFeePayer feePayer,
        @Schema(description = "Taken from each withdrawal when the earner pays the M-Pesa charge")
        @JsonProperty("earner_fee") BigDecimal earnerFee,
        @Schema(description = "Hours a newly added number is held before it can receive money")
        @JsonProperty("new_number_hold_hours") long newNumberHoldHours,
        @Schema(description = "False while withdrawals are switched off for everyone")
        @JsonProperty("withdrawals_enabled") boolean withdrawalsEnabled
) {

    public static PayoutRulesDTO from(PayoutSettings s, long holdHours, boolean enabled) {
        return new PayoutRulesDTO(s.getMinAmount(), s.getMaxAmount(), s.getDailyLimit(), s.getFeePayer(),
                s.getFeePayer() == PayoutFeePayer.EARNER ? s.getEarnerFee() : BigDecimal.ZERO, holdHours, enabled);
    }
}
