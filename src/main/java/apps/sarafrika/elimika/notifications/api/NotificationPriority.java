package apps.sarafrika.elimika.notifications.api;

/**
 * Priority levels for notification delivery.
 * 
 * Higher priority notifications are processed first and may override
 * certain user preferences (e.g., quiet hours for CRITICAL notifications).
 */
public enum NotificationPriority {
    
    LOW(1, "Low Priority", "Can be delayed or batched"),
    NORMAL(2, "Normal Priority", "Standard delivery timing"),
    HIGH(3, "High Priority", "Delivered promptly"),
    CRITICAL(4, "Critical Priority", "Immediate delivery, may override user preferences");
    
    private final int level;
    private final String displayName;
    private final String description;
    
    NotificationPriority(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
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
    
    public boolean isHigherThan(NotificationPriority other) {
        return this.level > other.level;
    }
    
    public boolean canOverrideUserPreferences() {
        return this == CRITICAL;
    }
}