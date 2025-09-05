package apps.sarafrika.elimika.notifications.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Priority levels for notification delivery.
 * 
 * Higher priority notifications are processed first and may override
 * certain user preferences (e.g., quiet hours for CRITICAL notifications).
 * Must match the database constraint: CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL'))
 */
public enum NotificationPriority {
    
    LOW("LOW", 1, "Low Priority", "Can be delayed or batched"),
    NORMAL("NORMAL", 2, "Normal Priority", "Standard delivery timing"),
    HIGH("HIGH", 3, "High Priority", "Delivered promptly"),
    CRITICAL("CRITICAL", 4, "Critical Priority", "Immediate delivery, may override user preferences");
    
    private final String value;
    private final int level;
    private final String displayName;
    private final String description;
    private static final Map<String, NotificationPriority> VALUE_MAP = new HashMap<>();
    
    static {
        for (NotificationPriority priority : NotificationPriority.values()) {
            VALUE_MAP.put(priority.value, priority);
            VALUE_MAP.put(priority.value.toLowerCase(), priority);
        }
    }
    
    NotificationPriority(String value, int level, String displayName, String description) {
        this.value = value;
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    @JsonCreator
    public static NotificationPriority fromValue(String value) {
        NotificationPriority priority = VALUE_MAP.get(value);
        if (priority == null) {
            throw new IllegalArgumentException("Unknown NotificationPriority: " + value);
        }
        return priority;
    }
    
    public static NotificationPriority fromString(String value) {
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
    public static NotificationPriority fromDatabaseValue(String value) {
        return fromValue(value);
    }
    
    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
    
    public boolean canOverrideUserPreferences() {
        return this == CRITICAL;
    }
}