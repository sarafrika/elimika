package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record RevenueDashboardDTO(
        @JsonProperty("domain")
        String domain,
        @JsonProperty("start_date")
        LocalDate startDate,
        @JsonProperty("end_date")
        LocalDate endDate,
        @JsonProperty("gross_totals")
        List<RevenueAmountDTO> grossTotals,
        @JsonProperty("estimated_earnings")
        List<RevenueAmountDTO> estimatedEarnings,
        @JsonProperty("order_count")
        long orderCount,
        @JsonProperty("line_item_count")
        long lineItemCount,
        @JsonProperty("units_sold")
        long unitsSold,
        @JsonProperty("average_order_value")
        List<RevenueAmountDTO> averageOrderValue,
        @JsonProperty("scope_breakdown")
        List<RevenueScopeBreakdownDTO> scopeBreakdown,
        @JsonProperty("daily_series")
        List<RevenueTimeSeriesPointDTO> dailySeries
) {
}
