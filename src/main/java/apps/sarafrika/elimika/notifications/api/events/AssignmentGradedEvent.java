package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when an assignment has been graded and feedback is available.
 * This notifies students that their work has been evaluated.
 */
public record AssignmentGradedEvent(
    UUID notificationId,
    UUID recipientId,
    String recipientEmail,
    String recipientName,
    UUID assignmentId,
    String assignmentTitle,
    String courseName,
    BigDecimal score,
    BigDecimal maxScore,
    String instructorName,
    String instructorComments,
    boolean hasFeedback,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public AssignmentGradedEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.ASSIGNMENT_GRADED;
    }
    
    @Override
    public NotificationPriority getPriority() {
        return NotificationPriority.NORMAL;
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
        variables.put("assignmentTitle", assignmentTitle);
        variables.put("courseName", courseName);
        variables.put("score", score);
        variables.put("maxScore", maxScore);
        variables.put("percentage", calculatePercentage());
        variables.put("instructorName", instructorName);
        variables.put("instructorComments", instructorComments);
        variables.put("hasFeedback", hasFeedback);
        variables.put("assignmentId", assignmentId);
        variables.put("gradeLevel", getGradeLevel());
        return variables;
    }
    
    private BigDecimal calculatePercentage() {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return score.divide(maxScore, 2, BigDecimal.ROUND_HALF_UP)
                   .multiply(BigDecimal.valueOf(100));
    }
    
    private String getGradeLevel() {
        BigDecimal percentage = calculatePercentage();
        if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) return "excellent";
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return "good";
        if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) return "satisfactory";
        if (percentage.compareTo(BigDecimal.valueOf(60)) >= 0) return "needs-improvement";
        return "below-expectations";
    }
}