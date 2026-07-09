package apps.sarafrika.elimika.notifications.api;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all notification events in the Elimika platform.
 * 
 * This event is published when any module needs to trigger a notification.
 * The notifications module listens to these events and handles delivery
 * through the appropriate channels based on user preferences.
 */
public interface NotificationEvent {
    
    /**
     * Unique identifier for this notification request
     */
    UUID getNotificationId();
    
    /**
     * The user who should receive this notification
     */
    UUID getRecipientId();
    
    /**
     * Email address of the recipient (for email notifications)
     */
    String getRecipientEmail();
    
    /**
     * Display name of the recipient
     */
    String getRecipientName();
    
    /**
     * Type of notification for template selection and user preferences
     */
    NotificationType getNotificationType();
    
    /**
     * Priority level for delivery urgency
     */
    NotificationPriority getPriority();
    
    /**
     * When this notification was requested
     */
    LocalDateTime getCreatedAt();
    
    /**
     * Optional: When to deliver this notification (for scheduling)
     */
    default LocalDateTime getScheduledFor() {
        return getCreatedAt();
    }
    
    /**
     * Template variables for dynamic content generation
     */
    Map<String, Object> getTemplateVariables();

    /**
     * Delivery channels requested by this event. Existing events default to email.
     */
    default Set<String> getDeliveryChannels() {
        return Set.of("email");
    }

    /**
     * How the notification should be presented in-app.
     */
    default NotificationPresentation getPresentation() {
        return NotificationPresentation.INBOX;
    }

    /**
     * The user domain (dashboard role) this notification is addressed to, so a
     * multi-domain recipient's inbox can be scoped to the active dashboard.
     * Defaults to {@code null}, in which case the notification type's default
     * audience ({@link NotificationType#getRecipientDomain()}) is used.
     */
    default String getRecipientDomain() {
        return null;
    }

    /**
     * In-app title. Defaults to the notification type display name.
     */
    default String getTitle() {
        return getNotificationType().getDisplayName();
    }

    /**
     * In-app body. Events should override this for user-facing inbox content.
     */
    default String getBody() {
        return getNotificationType().getDisplayName();
    }

    /**
     * Optional frontend route for click-through navigation.
     */
    default String getActionUrl() {
        return null;
    }

    /**
     * Stable idempotency key. Defaults to this event's notification identifier.
     */
    default String getDedupeKey() {
        return getNotificationId() != null ? getNotificationId().toString() : null;
    }
    
    /**
     * Optional: Organization context for branding and templates
     */
    default UUID getOrganizationId() {
        return null;
    }
    
    /**
     * Whether this notification can be batched with others
     */
    default boolean isBatchable() {
        return false;
    }
    
    /**
     * Language preference for localized content
     */
    default String getLanguage() {
        return "en";
    }
}
