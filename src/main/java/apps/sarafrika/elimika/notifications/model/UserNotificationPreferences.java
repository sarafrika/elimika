package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
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
    @Convert(converter = apps.sarafrika.elimika.notifications.util.converter.NotificationCategoryConverter.class)
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
    @Convert(converter = apps.sarafrika.elimika.notifications.util.converter.DigestModeConverter.class)
    @Builder.Default
    private DigestMode digestMode = DigestMode.IMMEDIATE;
    
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;
    
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;
    
    /**
     * How frequently notifications should be delivered
     * Must match the database constraint: CHECK (digest_mode IN ('IMMEDIATE', 'HOURLY', 'DAILY', 'WEEKLY', 'DISABLED'))
     */
    public enum DigestMode {
        IMMEDIATE("IMMEDIATE"),      // Send notifications immediately
        HOURLY("HOURLY"),            // Batch notifications into hourly summaries
        DAILY("DAILY"),              // Send daily digest emails
        WEEKLY("WEEKLY"),            // Send weekly digest emails
        DISABLED("DISABLED");        // Don't send notifications for this category
        
        private final String value;
        private static final java.util.Map<String, DigestMode> VALUE_MAP = new java.util.HashMap<>();
        
        static {
            for (DigestMode mode : DigestMode.values()) {
                VALUE_MAP.put(mode.value, mode);
                VALUE_MAP.put(mode.value.toLowerCase(), mode);
            }
        }
        
        DigestMode(String value) {
            this.value = value;
        }
        
        @com.fasterxml.jackson.annotation.JsonValue
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
        
        @com.fasterxml.jackson.annotation.JsonCreator
        public static DigestMode fromValue(String value) {
            DigestMode mode = VALUE_MAP.get(value);
            if (mode == null) {
                throw new IllegalArgumentException("Unknown DigestMode: " + value);
            }
            return mode;
        }
        
        public static DigestMode fromString(String value) {
            return fromValue(value);
        }
        
        /**
         * Get the database value (same as getValue())
         */
        public String getDatabaseValue() {
            return this.value;
        }
        
        /**
         * Create enum from database value (same as fromValue())
         */
        public static DigestMode fromDatabaseValue(String value) {
            return fromValue(value);
        }
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