package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RevenueScopeBreakdownDTO(
        @JsonProperty("scope")
        String scope,
        @JsonProperty("gross_totals")
        List<RevenueAmountDTO> grossTotals,
        @JsonProperty("estimated_earnings")
        List<RevenueAmountDTO> estimatedEarnings,
        @JsonProperty("line_item_count")
        long lineItemCount,
        @JsonProperty("units_sold")
        long unitsSold
) {
}
