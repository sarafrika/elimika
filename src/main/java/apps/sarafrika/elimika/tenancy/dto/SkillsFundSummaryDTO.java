package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "SkillsFundSummary", description = "Computed KPI roll-up for an organisation's skills fund.")
public record SkillsFundSummaryDTO(

        @Schema(description = "Total contributed across all funding sources.")
        @JsonProperty("total_balance")
        BigDecimal totalBalance,

        @Schema(description = "Amount allocated (approved/allocated transactions).")
        @JsonProperty("allocated")
        BigDecimal allocated,

        @Schema(description = "Amount disbursed (completed transactions).")
        @JsonProperty("disbursed")
        BigDecimal disbursed,

        @Schema(description = "Amount in pending requests.")
        @JsonProperty("pending")
        BigDecimal pending,

        @Schema(description = "Remaining = total balance − disbursed.")
        @JsonProperty("remaining")
        BigDecimal remaining
) {
}
