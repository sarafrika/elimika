package apps.sarafrika.elimika.notifications.internal;

import apps.sarafrika.elimika.notifications.api.NotificationCategory;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.model.NotificationPreferencesRepository;
import apps.sarafrika.elimika.notifications.model.UserNotificationPreferences;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.notifications.preferences.spi.exceptions.PreferencesAccessException;
import apps.sarafrika.elimika.notifications.preferences.spi.exceptions.PreferencesInitializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of NotificationPreferencesService SPI.
 * Handles creation of default preferences and preference queries.
 * 
 * @author Wilfred Njuguna
 * @since 2025-09-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesServiceImpl implements NotificationPreferencesService {
    
    private final NotificationPreferencesRepository preferencesRepository;
    
    @Override
    public boolean isNotificationEnabled(UUID userId, NotificationType type, String channel) 
            throws PreferencesAccessException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel cannot be null or empty");
        }
        
        try {
            NotificationCategory category = type.getCategory();
            UserNotificationPreferences preferences = findOrCreatePreferences(userId, category);
            return preferences.isChannelEnabled(channel);
        } catch (Exception e) {
            log.error("Failed to check notification enabled status for user {} and type {}: {}", 
                userId, type, e.getMessage(), e);
            throw new PreferencesAccessException(userId, "Error checking notification enabled status", e);
        }
    }
    
    /**
     * Safely find or create notification preferences
     */
    private UserNotificationPreferences findOrCreatePreferences(UUID userId, NotificationCategory category) {
        try {
            Optional<UserNotificationPreferences> existing = preferencesRepository
                .findByUserUuidAndCategory(userId, category);
            
            if (existing.isPresent()) {
                return existing.get();
            }
            
            // If no preferences exist for this user at all, initialize all categories
            if (!preferencesRepository.existsByUserUuid(userId)) {
                initializeUserPreferences(userId);
                // Try again after initialization
                return preferencesRepository.findByUserUuidAndCategory(userId, category)
                    .orElseThrow(() -> new RuntimeException("Failed to create preferences for user: " + userId));
            }
            
            // Create just this category's preferences
            return createDefaultPreferences(userId, category);
            
        } catch (Exception e) {
            log.error("Error finding/creating notification preferences for user {} and category {}: {}", 
                userId, category, e.getMessage(), e);
            
            // Return a default preference object without persisting to avoid further errors
            return UserNotificationPreferences.builder()
                .userUuid(userId)
                .category(category)
                .emailEnabled(getDefaultEmailSetting(category))
                .inAppEnabled(true)
                .smsEnabled(false)
                .pushEnabled(true)
                .digestMode(getDefaultDigestMode(category))
                .quietHoursStart(LocalTime.of(22, 0))
                .quietHoursEnd(LocalTime.of(8, 0))
                .build();
        }
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
    
    @Override
    public void initializeUserPreferences(UUID userId) throws PreferencesInitializationException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        log.info("Initializing notification preferences for user {}", userId);
        
        try {
            for (NotificationCategory category : NotificationCategory.values()) {
                if (!preferencesRepository.findByUserUuidAndCategory(userId, category).isPresent()) {
                    createDefaultPreferences(userId, category);
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize notification preferences for user {}: {}", userId, e.getMessage(), e);
            throw new PreferencesInitializationException(userId, "Failed to initialize notification preferences", e);
        }
    }
    
    @Override
    public boolean existsByUserUuid(UUID userId) throws PreferencesAccessException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            return preferencesRepository.existsByUserUuid(userId);
        } catch (Exception e) {
            log.error("Failed to check if preferences exist for user {}: {}", userId, e.getMessage(), e);
            throw new PreferencesAccessException(userId, "Failed to check preferences existence", e);
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