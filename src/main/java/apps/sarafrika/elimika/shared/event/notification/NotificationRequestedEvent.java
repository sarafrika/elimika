package apps.sarafrika.elimika.shared.event.notification;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record NotificationRequestedEvent(
        UUID notificationId,
        UUID recipientId,
        String recipientEmail,
        String recipientName,
        String notificationType,
        String priority,
        String presentation,
        String title,
        String body,
        String actionUrl,
        Map<String, Object> templateVariables,
        Set<String> deliveryChannels,
        String dedupeKey,
        LocalDateTime createdAt,
        UUID organizationId
) {

    public NotificationRequestedEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (priority == null || priority.isBlank()) {
            priority = "NORMAL";
        }
        if (presentation == null || presentation.isBlank()) {
            presentation = "INBOX";
        }
        templateVariables = templateVariables == null ? Map.of() : Map.copyOf(templateVariables);
        deliveryChannels = deliveryChannels == null ? Set.of("in_app") : Set.copyOf(deliveryChannels);
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public static NotificationRequestedEvent inApp(
            UUID recipientId,
            String notificationType,
            String presentation,
            String title,
            String body,
            String actionUrl,
            Map<String, Object> metadata,
            String dedupeKey
    ) {
        return new NotificationRequestedEvent(
                UUID.randomUUID(),
                recipientId,
                null,
                null,
                notificationType,
                "NORMAL",
                presentation,
                title,
                body,
                actionUrl,
                metadata,
                Set.of("in_app"),
                dedupeKey,
                LocalDateTime.now(ZoneOffset.UTC),
                null
        );
    }

    public static NotificationRequestedEvent email(
            UUID recipientId,
            String recipientEmail,
            String recipientName,
            String notificationType,
            Map<String, Object> templateVariables
    ) {
        return new NotificationRequestedEvent(
                UUID.randomUUID(),
                recipientId,
                recipientEmail,
                recipientName,
                notificationType,
                "NORMAL",
                "INBOX",
                null,
                null,
                null,
                templateVariables,
                Set.of("email"),
                null,
                LocalDateTime.now(ZoneOffset.UTC),
                null
        );
    }
}
