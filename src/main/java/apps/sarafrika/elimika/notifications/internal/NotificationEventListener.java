package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener that handles notification events from across the application.
 * This component bridges the gap between domain events and the notification system.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final NotificationService notificationService;
    
    /**
     * Handle all notification events published by other modules.
     * This method is called asynchronously to avoid blocking the publishing module.
     */
    @ApplicationModuleListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        log.debug("Received notification event: {} for user: {}", 
            event.getNotificationType(), event.getRecipientId());
        
        try {
            notificationService.sendNotification(event)
                .thenAccept(result -> {
                    if (result.isSuccessful()) {
                        log.info("Notification {} sent successfully via {}", 
                            event.getNotificationId(), result.channel());
                    } else {
                        log.warn("Notification {} failed: {} ({})", 
                            event.getNotificationId(), result.errorMessage(), result.status());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Failed to process notification event {}: {}", 
                        event.getNotificationId(), throwable.getMessage(), throwable);
                    return null;
                });
                
        } catch (Exception e) {
            log.error("Error handling notification event {}: {}", 
                event.getNotificationId(), e.getMessage(), e);
        }
    }
}