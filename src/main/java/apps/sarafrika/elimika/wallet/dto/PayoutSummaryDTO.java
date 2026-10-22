package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "PayoutSummary", description = "The admin Payouts page's headline figures")
public record PayoutSummaryDTO(
        @JsonProperty("needs_review_count") long needsReviewCount,
        @JsonProperty("needs_review_amount") BigDecimal needsReviewAmount,
        @JsonProperty("in_flight_count") long inFlightCount,
        @JsonProperty("paid_today_amount") BigDecimal paidTodayAmount,
        @JsonProperty("failed_count") long failedCount,
        @Schema(description = "M-Pesa payout float as last reported; null when unknown")
        @JsonProperty("float_amount") BigDecimal floatAmount,
        @JsonProperty("float_reported_at") LocalDateTime floatReportedAt,
        @Schema(description = "False while payouts are not configured (no gateway or payout shortcode)")
        @JsonProperty("payouts_configured") boolean payoutsConfigured
) {
}
