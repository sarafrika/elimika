package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record RevenueTimeSeriesPointDTO(
        @JsonProperty("date")
        LocalDate date,
        @JsonProperty("gross_totals")
        List<RevenueAmountDTO> grossTotals,
        @JsonProperty("estimated_earnings")
        List<RevenueAmountDTO> estimatedEarnings,
        @JsonProperty("order_count")
        long orderCount,
        @JsonProperty("units_sold")
        long unitsSold
) {
}
