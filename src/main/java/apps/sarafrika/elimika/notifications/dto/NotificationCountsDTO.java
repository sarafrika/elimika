package apps.sarafrika.elimika.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationCountsDTO(
        @JsonProperty("unread_count")
        long unreadCount,
        @JsonProperty("popup_count")
        long popupCount
) {
}
