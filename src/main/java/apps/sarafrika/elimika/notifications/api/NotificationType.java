package apps.sarafrika.elimika.notifications.api;

/**
 * Enumeration of notification types supported by the Elimika platform.
 * 
 * Each type corresponds to specific email templates and user preference categories.
 * This enum drives template selection, user preference filtering, and delivery routing.
 */
public enum NotificationType {
    
    // Learning Journey Notifications
    COURSE_ENROLLMENT_WELCOME("course-enrollment-welcome", "Course Enrollment", NotificationCategory.LEARNING_PROGRESS),
    COURSE_COMPLETION_CERTIFICATE("course-completion-certificate", "Course Completion", NotificationCategory.LEARNING_PROGRESS),
    LEARNING_MILESTONE_ACHIEVED("learning-milestone-achieved", "Learning Milestone", NotificationCategory.LEARNING_PROGRESS),
    
    // Assignment & Assessment Workflow
    ASSIGNMENT_DUE_REMINDER("assignment-due-reminder", "Assignment Due", NotificationCategory.ASSIGNMENTS_GRADING),
    ASSIGNMENT_SUBMITTED_CONFIRMATION("assignment-submitted-confirmation", "Assignment Submitted", NotificationCategory.ASSIGNMENTS_GRADING),
    ASSIGNMENT_GRADED("assignment-graded", "Assignment Graded", NotificationCategory.ASSIGNMENTS_GRADING),
    ASSIGNMENT_RETURNED_FOR_REVISION("assignment-returned-revision", "Assignment Revision", NotificationCategory.ASSIGNMENTS_GRADING),
    
    // Instructor Notifications
    NEW_STUDENT_ENROLLMENT("new-student-enrollment", "New Student", NotificationCategory.COURSE_MANAGEMENT),
    NEW_ASSIGNMENT_SUBMISSION("new-assignment-submission", "New Submission", NotificationCategory.ASSIGNMENTS_GRADING),
    GRADING_REMINDER("grading-reminder", "Grading Reminder", NotificationCategory.ASSIGNMENTS_GRADING),
    
    // Administrative Notifications
    USER_INVITATION_SENT("user-invitation-sent", "Invitation Sent", NotificationCategory.SYSTEM_ADMIN),
    INVITATION_ACCEPTED("invitation-accepted", "Invitation Accepted", NotificationCategory.SYSTEM_ADMIN),
    INVITATION_DECLINED("invitation-declined", "Invitation Declined", NotificationCategory.SYSTEM_ADMIN),
    INVITATION_EXPIRY_REMINDER("invitation-expiry-reminder", "Invitation Expiry", NotificationCategory.SYSTEM_ADMIN),
    
    // System Notifications
    ACCOUNT_CREATED("account-created", "Account Created", NotificationCategory.SYSTEM_ADMIN),
    PASSWORD_RESET_REQUEST("password-reset-request", "Password Reset", NotificationCategory.SYSTEM_ADMIN),
    SECURITY_ALERT("security-alert", "Security Alert", NotificationCategory.SYSTEM_ADMIN),
    
    // Engagement & Motivation
    WEEKLY_PROGRESS_SUMMARY("weekly-progress-summary", "Progress Summary", NotificationCategory.LEARNING_PROGRESS),
    LEARNING_STREAK_ACHIEVEMENT("learning-streak-achievement", "Streak Achievement", NotificationCategory.LEARNING_PROGRESS),
    PEER_ACHIEVEMENT_CELEBRATION("peer-achievement-celebration", "Peer Achievement", NotificationCategory.SOCIAL_LEARNING);
    
    private final String templateName;
    private final String displayName;
    private final NotificationCategory category;
    
    NotificationType(String templateName, String displayName, NotificationCategory category) {
        this.templateName = templateName;
        this.displayName = displayName;
        this.category = category;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public NotificationCategory getCategory() {
        return category;
    }
    
    /**
     * Get the email template file name for this notification type
     */
    public String getEmailTemplatePath() {
        return "email/" + templateName + ".html";
    }
}