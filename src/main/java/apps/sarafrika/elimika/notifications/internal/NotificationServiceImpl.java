package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.*;
import apps.sarafrika.elimika.notifications.model.NotificationPreferencesRepository;
import apps.sarafrika.elimika.notifications.model.UserNotificationPreferences;
import apps.sarafrika.elimika.notifications.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Internal implementation of the NotificationService.
 * Handles routing notifications to appropriate channels based on user preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private final EmailNotificationService emailNotificationService;
    private final NotificationPreferencesRepository preferencesRepository;
    private final NotificationPreferencesService preferencesService;
    
    @Override
    public CompletableFuture<NotificationResult> sendNotification(NotificationEvent event) {
        return sendNotification(event, DeliveryOptions.defaults());
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendNotification(NotificationEvent event, DeliveryOptions options) {
        log.debug("Processing notification {} of type {} for user {}", 
            event.getNotificationId(), event.getNotificationType(), event.getRecipientId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if user has enabled notifications for this category
                if (!options.bypassPreferences() && !isNotificationAllowed(event, options)) {
                    log.debug("Notification {} blocked by user preferences", event.getNotificationId());
                    return NotificationResult.blocked(event.getNotificationId(), "email");
                }
                
                // Check quiet hours
                if (options.respectQuietHours() && isInQuietHours(event.getRecipientId())) {
                    log.debug("Notification {} blocked by quiet hours", event.getNotificationId());
                    return NotificationResult.blocked(event.getNotificationId(), "email");
                }
                
                // For MVP, we only support email notifications
                return sendEmailNotification(event).join();
                
            } catch (Exception e) {
                log.error("Failed to process notification {}: {}", event.getNotificationId(), e.getMessage(), e);
                return NotificationResult.failed(event.getNotificationId(), "email", e.getMessage());
            }
        });
    }
    
    @Override
    public boolean isNotificationEnabled(UUID userId, NotificationType type) {
        return preferencesService.isNotificationEnabled(userId, type, "email");
    }
    
    @Override
    public NotificationResult getDeliveryStatus(UUID notificationId) {
        // For MVP, we'll implement basic status checking
        // In production, this would query the delivery log
        return NotificationResult.pending(notificationId, "email");
    }
    
    /**
     * Send email notification
     */
    private CompletableFuture<NotificationResult> sendEmailNotification(NotificationEvent event) {
        return emailNotificationService.sendEmail(event);
    }
    
    /**
     * Check if notification is allowed based on user preferences
     */
    private boolean isNotificationAllowed(NotificationEvent event, DeliveryOptions options) {
        // If specific channels are forced, allow it
        if (options.forceChannels().contains("email")) {
            return true;
        }
        
        // Check user preferences
        return preferencesService.isNotificationEnabled(
            event.getRecipientId(), 
            event.getNotificationType(), 
            "email"
        );
    }
    
    /**
     * Check if current time is within user's quiet hours
     */
    private boolean isInQuietHours(UUID userId) {
        try {
            return preferencesService.isInQuietHours(userId, LocalTime.now());
        } catch (Exception e) {
            log.warn("Failed to check quiet hours for user {}: {}", userId, e.getMessage());
            return false; // Default to allowing notifications if we can't check preferences
        }
    }
}