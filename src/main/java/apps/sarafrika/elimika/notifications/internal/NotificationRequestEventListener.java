package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationService;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRequestEventListener {

    private final NotificationService notificationService;

    @ApplicationModuleListener
    void onNotificationRequested(NotificationRequestedEvent request) {
        RequestedNotificationEvent event = RequestedNotificationEvent.from(request);
        notificationService.sendNotification(event)
                .exceptionally(throwable -> {
                    log.error("Failed to process notification request {}: {}",
                            request.notificationId(), throwable.getMessage(), throwable);
                    return null;
                });
    }

    private record RequestedNotificationEvent(
            UUID notificationId,
            UUID recipientId,
            String recipientEmail,
            String recipientName,
            NotificationType notificationType,
            NotificationPriority priority,
            NotificationPresentation presentation,
            String title,
            String body,
            String actionUrl,
            Map<String, Object> templateVariables,
            Set<String> deliveryChannels,
            String dedupeKey,
            LocalDateTime createdAt,
            UUID organizationId
    ) implements NotificationEvent {

        static RequestedNotificationEvent from(NotificationRequestedEvent request) {
            return new RequestedNotificationEvent(
                    request.notificationId(),
                    request.recipientId(),
                    request.recipientEmail(),
                    request.recipientName(),
                    NotificationType.fromValue(request.notificationType().toUpperCase(Locale.ROOT)),
                    NotificationPriority.fromValue(request.priority().toUpperCase(Locale.ROOT)),
                    NotificationPresentation.fromValue(request.presentation().toUpperCase(Locale.ROOT)),
                    request.title(),
                    request.body(),
                    request.actionUrl(),
                    request.templateVariables(),
                    request.deliveryChannels(),
                    request.dedupeKey(),
                    request.createdAt(),
                    request.organizationId()
            );
        }

        @Override
        public UUID getNotificationId() {
            return notificationId;
        }

        @Override
        public UUID getRecipientId() {
            return recipientId;
        }

        @Override
        public String getRecipientEmail() {
            return recipientEmail;
        }

        @Override
        public String getRecipientName() {
            return recipientName;
        }

        @Override
        public NotificationType getNotificationType() {
            return notificationType;
        }

        @Override
        public NotificationPriority getPriority() {
            return priority;
        }

        @Override
        public Map<String, Object> getTemplateVariables() {
            return templateVariables;
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        @Override
        public UUID getOrganizationId() {
            return organizationId;
        }

        @Override
        public Set<String> getDeliveryChannels() {
            return deliveryChannels;
        }

        @Override
        public NotificationPresentation getPresentation() {
            return presentation;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public String getActionUrl() {
            return actionUrl;
        }

        @Override
        public String getDedupeKey() {
            return dedupeKey;
        }
    }
}
