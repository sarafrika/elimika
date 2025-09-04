package apps.sarafrika.elimika.notifications.preferences.spi.exceptions;

/**
 * Base exception for notification preferences operations.
 * 
 * @author Wilfred Njuguna
 * @since 2025-09-04
 */
public class PreferencesException extends RuntimeException {
    
    public PreferencesException(String message) {
        super(message);
    }
    
    public PreferencesException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PreferencesException(Throwable cause) {
        super(cause);
    }
}