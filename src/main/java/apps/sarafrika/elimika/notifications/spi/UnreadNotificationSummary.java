package apps.sarafrika.elimika.notifications.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** Account-wide totals plus per-domain counts; each domain also counts account-level notifications. */
public record UnreadNotificationSummary(
        @JsonProperty("unread_count")
        long unreadCount,
        @JsonProperty("popup_count")
        long popupCount,
        @JsonProperty("by_domain")
        Map<String, DomainNotificationCounts> byDomain
) {
}
