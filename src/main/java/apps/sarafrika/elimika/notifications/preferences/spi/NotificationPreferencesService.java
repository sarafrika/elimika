package apps.sarafrika.elimika.notifications.preferences.spi;

import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.preferences.spi.exceptions.PreferencesAccessException;
import apps.sarafrika.elimika.notifications.preferences.spi.exceptions.PreferencesInitializationException;

import java.util.UUID;

/**
 * Service Provider Interface for managing user notification preferences.
 * 
 * This interface provides a way for other modules to interact with notification
 * preferences without direct dependencies on the internal implementation.
 * 
 * All operations are synchronous and provide immediate feedback through return values
 * or exceptions. This service handles both read and write operations for notification
 * preferences in a modular monolith architecture.
 * 
 * @author Wilfred Njuguna
 * @since 2025-09-04
 */
public interface NotificationPreferencesService {
    
    /**
     * Initialize default notification preferences for a new user.
     * 
     * Creates default preferences for all notification categories if they don't exist.
     * This method is idempotent - if preferences already exist for a category, they won't be overwritten.
     * 
     * @param userId the UUID of the user to initialize preferences for
     * @throws PreferencesInitializationException if the initialization fails due to database errors,
     *         validation failures, or other system issues
     * @throws IllegalArgumentException if userId is null
     */
    void initializeUserPreferences(UUID userId) throws PreferencesInitializationException;
    
    /**
     * Check if a user has any notification preferences configured.
     * 
     * This is a lightweight check that returns quickly without loading full preference data.
     * Useful for determining if a user needs default preferences initialization.
     * 
     * @param userId the UUID of the user to check
     * @return true if the user has any notification preferences, false otherwise
     * @throws PreferencesAccessException if there's an error accessing the preferences data
     * @throws IllegalArgumentException if userId is null
     */
    boolean existsByUserUuid(UUID userId) throws PreferencesAccessException;
    
    /**
     * Check if a notification is enabled for a user and specific delivery channel.
     * 
     * If no preferences exist for the user, this method will create default preferences
     * and return the appropriate default setting. This provides a seamless experience
     * where notifications work out-of-the-box with sensible defaults.
     * 
     * @param userId the UUID of the user
     * @param notificationType the type of notification to check
     * @param channel the delivery channel (email, sms, push, in_app)
     * @return true if the notification is enabled for the specified channel, false otherwise
     * @throws PreferencesAccessException if there's an error accessing or creating preferences
     * @throws IllegalArgumentException if any parameter is null or if channel is not recognized
     */
    boolean isNotificationEnabled(UUID userId, NotificationType notificationType, String channel) 
            throws PreferencesAccessException;
}