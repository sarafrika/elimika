package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.notifications.api.DeliveryStatus;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking notification delivery attempts and results.
 * Provides audit trail and analytics capabilities for the notification system.
 */
@Entity
@Table(name = "notification_delivery_log")
@Getter @Setter @ToString
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public class NotificationDeliveryLog extends BaseEntity {
    
    @Column(name = "notification_id", nullable = false, unique = true)
    private UUID notificationId;
    
    @Column(name = "user_uuid", nullable = false)
    private UUID userUuid;
    
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Column(name = "notification_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
    
    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;
    
    @Column(name = "delivery_channel", nullable = false)
    private String deliveryChannel;
    
    @Column(name = "delivery_status", nullable = false)
    @Convert(converter = apps.sarafrika.elimika.notifications.util.converter.DeliveryStatusConverter.class)
    private DeliveryStatus deliveryStatus;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "template_used")
    private String templateUsed;
    
    @Column(name = "organization_uuid")
    private UUID organizationUuid;
    
    /**
     * Mark this notification as successfully delivered
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }
    
    /**
     * Mark this notification as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.deliveryStatus = DeliveryStatus.FAILED;
        this.errorMessage = errorMessage;
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }
    
    /**
     * Increment retry count for failed delivery
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }
    
    /**
     * Check if this notification can be retried based on retry count
     */
    public boolean canRetry(int maxRetries) {
        return retryCount != null && retryCount < maxRetries;
    }
}