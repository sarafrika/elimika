package apps.sarafrika.elimika.notifications.dto;

import apps.sarafrika.elimika.notifications.api.NotificationCategory;
import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationDTO(
        @JsonProperty("uuid")
        UUID uuid,
        @JsonProperty("notification_id")
        UUID notificationId,
        @JsonProperty("type")
        NotificationType type,
        @JsonProperty("category")
        NotificationCategory category,
        @JsonProperty("priority")
        NotificationPriority priority,
        @JsonProperty("presentation")
        NotificationPresentation presentation,
        @JsonProperty("status")
        UserNotificationStatus status,
        @JsonProperty("title")
        String title,
        @JsonProperty("body")
        String body,
        @JsonProperty("action_url")
        String actionUrl,
        @JsonProperty("metadata")
        Map<String, Object> metadata,
        @JsonProperty("occurred_at")
        LocalDateTime occurredAt,
        @JsonProperty("popup_seen_at")
        LocalDateTime popupSeenAt,
        @JsonProperty("read_at")
        LocalDateTime readAt,
        @JsonProperty("archived_at")
        LocalDateTime archivedAt,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}
