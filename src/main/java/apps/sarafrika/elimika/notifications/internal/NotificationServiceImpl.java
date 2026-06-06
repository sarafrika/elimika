package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.*;
import apps.sarafrika.elimika.notifications.model.NotificationPreferencesRepository;
import apps.sarafrika.elimika.notifications.model.UserNotificationPreferences;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.notifications.service.EmailNotificationService;
import apps.sarafrika.elimika.notifications.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final UserNotificationService userNotificationService;
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
                boolean delivered = false;
                NotificationResult lastResult = NotificationResult.pending(event.getNotificationId(), "notification");

                if (event.getDeliveryChannels().contains("in_app")) {
                    if (options.bypassPreferences() || isNotificationAllowed(event, "in_app", options)) {
                        userNotificationService.createFromEvent(event);
                        delivered = true;
                        lastResult = NotificationResult.success(event.getNotificationId(), "in_app");
                    } else {
                        log.debug("In-app notification {} blocked by user preferences", event.getNotificationId());
                        lastResult = NotificationResult.blocked(event.getNotificationId(), "in_app");
                    }
                }

                if (event.getDeliveryChannels().contains("email")) {
                    if (!StringUtils.hasText(event.getRecipientEmail())) {
                        log.warn("Email notification {} skipped because recipient email is missing", event.getNotificationId());
                    } else if (!options.bypassPreferences() && !isNotificationAllowed(event, "email", options)) {
                        log.debug("Email notification {} blocked by user preferences", event.getNotificationId());
                        lastResult = NotificationResult.blocked(event.getNotificationId(), "email");
                    } else if (options.respectQuietHours() && isInQuietHours(event.getRecipientId())) {
                        log.debug("Email notification {} blocked by quiet hours", event.getNotificationId());
                        lastResult = NotificationResult.blocked(event.getNotificationId(), "email");
                    } else {
                        lastResult = sendEmailNotification(event).join();
                        delivered = delivered || lastResult.isSuccessful();
                    }
                }

                return delivered ? NotificationResult.success(event.getNotificationId(), lastResult.channel()) : lastResult;
                
            } catch (Exception e) {
                log.error("Failed to process notification {}: {}", event.getNotificationId(), e.getMessage(), e);
                return NotificationResult.failed(event.getNotificationId(), "notification", e.getMessage());
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
    private boolean isNotificationAllowed(NotificationEvent event, String channel, DeliveryOptions options) {
        // If specific channels are forced, allow it
        if (options.forceChannels().contains(channel)) {
            return true;
        }
        
        // Check user preferences
        return preferencesService.isNotificationEnabled(
            event.getRecipientId(), 
            event.getNotificationType(), 
            channel
        );
    }
    
    /**
     * Check if current time is within user's quiet hours
     */
    private boolean isInQuietHours(UUID userId) {
        try {
            // For now, we'll directly query the repository for quiet hours
            // This could be moved to the SPI interface in the future
            var preferences = preferencesRepository.findByUserUuid(userId);
            if (preferences.isEmpty()) {
                return false;
            }
            
            LocalTime currentTime = LocalTime.now();
            return preferences.stream()
                .anyMatch(pref -> pref.isInQuietHours(currentTime));
        } catch (Exception e) {
            log.warn("Failed to check quiet hours for user {}: {}", userId, e.getMessage());
            return false; // Default to allowing notifications if we can't check preferences
        }
    }
}
