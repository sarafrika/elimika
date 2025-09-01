package apps.sarafrika.elimika.notifications.api;

/**
 * Categories of notifications for user preference management.
 * 
 * Users can control their notification preferences at the category level,
 * allowing granular control over what types of notifications they receive.
 */
public enum NotificationCategory {
    
    LEARNING_PROGRESS("Learning Progress & Achievements", 
        "Notifications about your learning milestones, course progress, and achievements"),
    
    ASSIGNMENTS_GRADING("Assignments & Grading", 
        "Notifications about assignment due dates, submissions, grading, and feedback"),
    
    COURSE_MANAGEMENT("Course Management", 
        "Notifications for instructors about course enrollments, content, and student activity"),
    
    SOCIAL_LEARNING("Social Learning & Community", 
        "Notifications about peer activities, study groups, and community interactions"),
    
    SYSTEM_ADMIN("System & Administrative", 
        "Account notifications, invitations, security alerts, and system updates");
    
    private final String displayName;
    private final String description;
    
    NotificationCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}