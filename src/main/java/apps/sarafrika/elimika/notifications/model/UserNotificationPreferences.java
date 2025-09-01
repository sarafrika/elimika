package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.notifications.api.NotificationCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Entity representing user preferences for notifications.
 * Users can control what types of notifications they receive and how.
 */
@Entity
@Table(name = "user_notification_preferences")
@Getter @Setter @ToString
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public class UserNotificationPreferences extends BaseEntity {
    
    @Column(name = "user_uuid", nullable = false)
    private UUID userUuid;
    
    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationCategory category;
    
    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;
    
    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;
    
    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;
    
    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;
    
    @Column(name = "digest_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DigestMode digestMode = DigestMode.IMMEDIATE;
    
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;
    
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;
    
    /**
     * How frequently notifications should be delivered
     */
    public enum DigestMode {
        IMMEDIATE,      // Send notifications immediately
        HOURLY,         // Batch notifications into hourly summaries
        DAILY,          // Send daily digest emails
        WEEKLY,         // Send weekly digest emails
        DISABLED        // Don't send notifications for this category
    }
    
    /**
     * Check if notifications are allowed during quiet hours
     */
    public boolean isInQuietHours(LocalTime currentTime) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            // Same day quiet hours (e.g., 22:00 - 08:00 next day)
            return currentTime.isAfter(quietHoursStart) || currentTime.isBefore(quietHoursEnd);
        } else {
            // Overnight quiet hours (e.g., 10:00 - 16:00 same day)
            return currentTime.isAfter(quietHoursStart) && currentTime.isBefore(quietHoursEnd);
        }
    }
    
    /**
     * Check if a specific notification channel is enabled
     */
    public boolean isChannelEnabled(String channel) {
        return switch (channel.toLowerCase()) {
            case "email" -> emailEnabled;
            case "in_app", "in-app" -> inAppEnabled;
            case "sms" -> smsEnabled;
            case "push" -> pushEnabled;
            default -> false;
        };
    }
}