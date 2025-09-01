package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a student enrolls in a course.
 * This sends a welcome email with course information and getting started guide.
 */
public record CourseEnrollmentWelcomeEvent(
    UUID notificationId,
    UUID recipientId,
    String recipientEmail,
    String recipientName,
    UUID courseId,
    String courseName,
    String courseDescription,
    String instructorName,
    LocalDateTime courseStartDate,
    int estimatedDurationWeeks,
    String courseImageUrl,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public CourseEnrollmentWelcomeEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.COURSE_ENROLLMENT_WELCOME;
    }
    
    @Override
    public NotificationPriority getPriority() {
        return NotificationPriority.HIGH; // Welcome messages are important for engagement
    }
    
    @Override
    public UUID getNotificationId() {
        return notificationId;
    }
    
    @Override
    public UUID getRecipientId() {
        return recipientId;
    }
    
    @Override
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    @Override
    public String getRecipientName() {
        return recipientName;
    }
    
    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public UUID getOrganizationId() {
        return organizationId;
    }
    
    @Override
    public Map<String, Object> getTemplateVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", recipientName);
        variables.put("courseName", courseName);
        variables.put("courseDescription", courseDescription);
        variables.put("instructorName", instructorName);
        variables.put("courseStartDate", courseStartDate);
        variables.put("estimatedDurationWeeks", estimatedDurationWeeks);
        variables.put("courseImageUrl", courseImageUrl);
        variables.put("courseId", courseId);
        variables.put("isStartingSoon", isCourseStartingWithin(7));
        variables.put("welcomeMessage", getPersonalizedWelcomeMessage());
        return variables;
    }
    
    private boolean isCourseStartingWithin(int days) {
        if (courseStartDate == null) return false;
        return courseStartDate.isBefore(LocalDateTime.now().plusDays(days));
    }
    
    private String getPersonalizedWelcomeMessage() {
        if (isCourseStartingWithin(3)) {
            return "Your course starts very soon! Get ready to begin your learning journey.";
        } else if (isCourseStartingWithin(7)) {
            return "Your course starts soon! Take some time to explore the course materials.";
        } else {
            return "Welcome to your new course! You can start exploring the content right away.";
        }
    }
}