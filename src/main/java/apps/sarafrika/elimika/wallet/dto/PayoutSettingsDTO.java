package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.entity.PayoutSettings;
import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "PayoutSettings", description = "Rules every withdrawal follows")
public record PayoutSettingsDTO(
        @NotNull @Positive @JsonProperty("min_amount") BigDecimal minAmount,
        @NotNull @Positive @JsonProperty("max_amount") BigDecimal maxAmount,
        @NotNull @Positive @JsonProperty("daily_limit") BigDecimal dailyLimit,
        @Schema(description = "Withdrawals above this wait for an admin")
        @NotNull @Positive @JsonProperty("review_limit") BigDecimal reviewLimit,
        @Schema(description = "Organisation withdrawals above this need a second payout approver")
        @NotNull @Positive @JsonProperty("second_approval_limit") BigDecimal secondApprovalLimit,
        @NotNull @JsonProperty("fee_payer") PayoutFeePayer feePayer,
        @Schema(description = "Charged per withdrawal when the earner pays the M-Pesa charge")
        @PositiveOrZero @JsonProperty("earner_fee") BigDecimal earnerFee,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) @JsonProperty("updated_date") OffsetDateTime updatedDate
) {

    public static PayoutSettingsDTO from(PayoutSettings s) {
        return new PayoutSettingsDTO(s.getMinAmount(), s.getMaxAmount(), s.getDailyLimit(), s.getReviewLimit(),
                s.getSecondApprovalLimit(), s.getFeePayer(), s.getEarnerFee(), s.getUpdatedDate());
    }
}
