package apps.sarafrika.elimika.notifications.api;

import java.time.LocalDateTime;
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