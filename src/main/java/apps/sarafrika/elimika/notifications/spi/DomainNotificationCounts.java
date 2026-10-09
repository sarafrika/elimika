package apps.sarafrika.elimika.notifications.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Unread and unseen-popup counts as {@code GET /notifications/counts?domain=} reports them. */
public record DomainNotificationCounts(
        @JsonProperty("unread_count")
        long unreadCount,
        @JsonProperty("popup_count")
        long popupCount
) {
}
