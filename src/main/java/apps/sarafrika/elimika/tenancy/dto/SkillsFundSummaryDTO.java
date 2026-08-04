package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "SkillsFundSummary", description = "Computed KPI roll-up for an organisation's skills fund.")
public record SkillsFundSummaryDTO(

        @Schema(description = "Total contributed across all funding sources.")
        @JsonProperty("total_balance")
        BigDecimal totalBalance,

        @Schema(description = "Amount committed against the fund — allocated, approved or already disbursed. "
                + "Cumulative: money that has gone out was committed first, so it is counted here too.")
        @JsonProperty("allocated")
        BigDecimal allocated,

        @Schema(description = "Amount that has actually left the fund (DISBURSED movements).")
        @JsonProperty("disbursed")
        BigDecimal disbursed,

        @Schema(description = "Amount in pending requests.")
        @JsonProperty("pending")
        BigDecimal pending,

        @Schema(description = "Remaining = total balance − disbursed.")
        @JsonProperty("remaining")
        BigDecimal remaining,

        @Schema(description = "ISO-4217 currency every figure above is denominated in. A fund holds exactly "
                + "one currency; these totals are meaningless without it.")
        @JsonProperty("currency_code")
        String currencyCode
) {
}
