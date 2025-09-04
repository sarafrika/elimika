package apps.sarafrika.elimika.notifications.preferences.spi.exceptions;

import java.util.UUID;

/**
 * Exception thrown when there's an error initializing user notification preferences.
 * 
 * @author Wilfred Njuguna
 * @since 2025-09-04
 */
public class PreferencesInitializationException extends PreferencesException {
    
    private final UUID userId;
    
    public PreferencesInitializationException(UUID userId, String message) {
        super(String.format("Failed to initialize preferences for user %s: %s", userId, message));
        this.userId = userId;
    }
    
    public PreferencesInitializationException(UUID userId, String message, Throwable cause) {
        super(String.format("Failed to initialize preferences for user %s: %s", userId, message), cause);
        this.userId = userId;
    }
    
    public UUID getUserId() {
        return userId;
    }
}