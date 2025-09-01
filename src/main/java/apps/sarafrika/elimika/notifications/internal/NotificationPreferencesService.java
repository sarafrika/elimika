package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.NotificationCategory;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.model.NotificationPreferencesRepository;
import apps.sarafrika.elimika.notifications.model.UserNotificationPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user notification preferences.
 * Handles creation of default preferences and preference queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesService {
    
    private final NotificationPreferencesRepository preferencesRepository;
    
    /**
     * Check if a notification is enabled for a user
     */
    public boolean isNotificationEnabled(UUID userId, NotificationType type, String channel) {
        NotificationCategory category = type.getCategory();
        
        UserNotificationPreferences preferences = preferencesRepository
            .findByUserUuidAndCategory(userId, category)
            .orElseGet(() -> createDefaultPreferences(userId, category));
        
        return preferences.isChannelEnabled(channel);
    }
    
    /**
     * Check if current time is in user's quiet hours
     */
    public boolean isInQuietHours(UUID userId, LocalTime currentTime) {
        List<UserNotificationPreferences> allPreferences = preferencesRepository.findByUserUuid(userId);
        
        // If no preferences exist, not in quiet hours
        if (allPreferences.isEmpty()) {
            return false;
        }
        
        // Check if any preference has quiet hours that include current time
        return allPreferences.stream()
            .anyMatch(pref -> pref.isInQuietHours(currentTime));
    }
    
    /**
     * Create default notification preferences for a user and category
     */
    public UserNotificationPreferences createDefaultPreferences(UUID userId, NotificationCategory category) {
        log.debug("Creating default preferences for user {} and category {}", userId, category);
        
        UserNotificationPreferences preferences = UserNotificationPreferences.builder()
            .userUuid(userId)
            .category(category)
            .emailEnabled(getDefaultEmailSetting(category))
            .inAppEnabled(true)
            .smsEnabled(false)
            .pushEnabled(true)
            .digestMode(getDefaultDigestMode(category))
            .quietHoursStart(LocalTime.of(22, 0))  // 10 PM
            .quietHoursEnd(LocalTime.of(8, 0))     // 8 AM
            .build();
        
        return preferencesRepository.save(preferences);
    }
    
    /**
     * Initialize all default preferences for a new user
     */
    public void initializeUserPreferences(UUID userId) {
        log.info("Initializing notification preferences for user {}", userId);
        
        for (NotificationCategory category : NotificationCategory.values()) {
            if (!preferencesRepository.findByUserUuidAndCategory(userId, category).isPresent()) {
                createDefaultPreferences(userId, category);
            }
        }
    }
    
    /**
     * Get default email setting based on category
     */
    private boolean getDefaultEmailSetting(NotificationCategory category) {
        return switch (category) {
            case ASSIGNMENTS_GRADING -> true;      // Always enable for assignments
            case LEARNING_PROGRESS -> true;       // Enable for progress updates
            case COURSE_MANAGEMENT -> true;       // Enable for instructors
            case SYSTEM_ADMIN -> true;            // Enable for system notifications
            case SOCIAL_LEARNING -> false;       // Disable by default for social
        };
    }
    
    /**
     * Get default digest mode based on category
     */
    private UserNotificationPreferences.DigestMode getDefaultDigestMode(NotificationCategory category) {
        return switch (category) {
            case ASSIGNMENTS_GRADING -> UserNotificationPreferences.DigestMode.IMMEDIATE;
            case SYSTEM_ADMIN -> UserNotificationPreferences.DigestMode.IMMEDIATE;
            case LEARNING_PROGRESS -> UserNotificationPreferences.DigestMode.IMMEDIATE;
            case COURSE_MANAGEMENT -> UserNotificationPreferences.DigestMode.IMMEDIATE;
            case SOCIAL_LEARNING -> UserNotificationPreferences.DigestMode.DAILY;
        };
    }
}