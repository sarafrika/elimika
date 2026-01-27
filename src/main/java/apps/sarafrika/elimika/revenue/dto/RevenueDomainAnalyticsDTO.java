package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevenueDomainAnalyticsDTO(
        @JsonProperty("domain")
        String domain,
        @JsonProperty("dashboard")
        RevenueDashboardDTO dashboard
) {
}
