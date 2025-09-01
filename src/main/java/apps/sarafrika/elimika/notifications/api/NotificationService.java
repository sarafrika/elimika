package apps.sarafrika.elimika.notifications.api;

import java.util.concurrent.CompletableFuture;

/**
 * Service Provider Interface (SPI) for the notifications module.
 * 
 * This interface provides the public API that other modules can use to send notifications.
 * The implementation is internal to the notifications module, following Spring Modulith principles.
 * 
 * Other modules should not directly implement this interface but should publish
 * NotificationEvent instances which are automatically handled by the notification system.
 */
public interface NotificationService {
    
    /**
     * Send a notification immediately.
     * This method processes the notification event and delivers it through appropriate channels
     * based on the notification type and user preferences.
     * 
     * @param event the notification event to process
     * @return a future that completes when the notification has been queued for delivery
     */
    CompletableFuture<NotificationResult> sendNotification(NotificationEvent event);
    
    /**
     * Send a notification with custom delivery options.
     * This allows overriding default behavior for specific use cases.
     * 
     * @param event the notification event to process
     * @param options delivery options to override defaults
     * @return a future that completes when the notification has been queued for delivery
     */
    CompletableFuture<NotificationResult> sendNotification(NotificationEvent event, DeliveryOptions options);
    
    /**
     * Check if a user has notifications enabled for a specific type.
     * This is useful for conditional notification sending.
     * 
     * @param userId the user to check
     * @param type the notification type
     * @return true if the user accepts this type of notification
     */
    boolean isNotificationEnabled(java.util.UUID userId, NotificationType type);
    
    /**
     * Get delivery status for a previously sent notification.
     * 
     * @param notificationId the notification ID from the original event
     * @return the current delivery status
     */
    NotificationResult getDeliveryStatus(java.util.UUID notificationId);
}