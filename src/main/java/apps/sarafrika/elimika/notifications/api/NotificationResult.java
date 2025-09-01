package apps.sarafrika.elimika.notifications.api;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result of a notification delivery attempt.
 * Provides information about the delivery status and any relevant details.
 */
public record NotificationResult(
    UUID notificationId,
    DeliveryStatus status,
    String channel,
    LocalDateTime sentAt,
    LocalDateTime deliveredAt,
    String errorMessage,
    int retryCount
) {
    
    public static NotificationResult success(UUID notificationId, String channel) {
        return new NotificationResult(
            notificationId,
            DeliveryStatus.DELIVERED,
            channel,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            0
        );
    }
    
    public static NotificationResult pending(UUID notificationId, String channel) {
        return new NotificationResult(
            notificationId,
            DeliveryStatus.PENDING,
            channel,
            LocalDateTime.now(),
            null,
            null,
            0
        );
    }
    
    public static NotificationResult failed(UUID notificationId, String channel, String errorMessage) {
        return new NotificationResult(
            notificationId,
            DeliveryStatus.FAILED,
            channel,
            LocalDateTime.now(),
            null,
            errorMessage,
            0
        );
    }
    
    public static NotificationResult blocked(UUID notificationId, String channel) {
        return new NotificationResult(
            notificationId,
            DeliveryStatus.BLOCKED,
            channel,
            LocalDateTime.now(),
            null,
            "Blocked by user preferences",
            0
        );
    }
    
    public boolean isSuccessful() {
        return status == DeliveryStatus.DELIVERED;
    }
    
    public boolean isPending() {
        return status == DeliveryStatus.PENDING || status == DeliveryStatus.QUEUED;
    }
    
    public boolean hasFailed() {
        return status == DeliveryStatus.FAILED;
    }
}