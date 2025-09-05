package apps.sarafrika.elimika.notifications.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Categories of notifications for user preference management.
 * 
 * Users can control their notification preferences at the category level,
 * allowing granular control over what types of notifications they receive.
 * Must match the database constraint: CHECK (category IN ('LEARNING_PROGRESS', 'ASSIGNMENTS_GRADING', 'COURSE_MANAGEMENT', 'SOCIAL_LEARNING', 'SYSTEM_ADMIN'))
 */
public enum NotificationCategory {
    
    LEARNING_PROGRESS("LEARNING_PROGRESS", "Learning Progress & Achievements", 
        "Notifications about your learning milestones, course progress, and achievements"),
    
    ASSIGNMENTS_GRADING("ASSIGNMENTS_GRADING", "Assignments & Grading", 
        "Notifications about assignment due dates, submissions, grading, and feedback"),
    
    COURSE_MANAGEMENT("COURSE_MANAGEMENT", "Course Management", 
        "Notifications for instructors about course enrollments, content, and student activity"),
    
    SOCIAL_LEARNING("SOCIAL_LEARNING", "Social Learning & Community", 
        "Notifications about peer activities, study groups, and community interactions"),
    
    SYSTEM_ADMIN("SYSTEM_ADMIN", "System & Administrative", 
        "Account notifications, invitations, security alerts, and system updates");
    
    private final String value;
    private final String displayName;
    private final String description;
    private static final Map<String, NotificationCategory> VALUE_MAP = new HashMap<>();
    
    static {
        for (NotificationCategory category : NotificationCategory.values()) {
            VALUE_MAP.put(category.value, category);
            VALUE_MAP.put(category.value.toLowerCase(), category);
        }
    }
    
    NotificationCategory(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }
    
    @JsonValue
    public String getValue() {
        return value;
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
    public static NotificationCategory fromValue(String value) {
        NotificationCategory category = VALUE_MAP.get(value);
        if (category == null) {
            throw new IllegalArgumentException("Unknown NotificationCategory: " + value);
        }
        return category;
    }
    
    public static NotificationCategory fromString(String value) {
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
    public static NotificationCategory fromDatabaseValue(String value) {
        return fromValue(value);
    }
}