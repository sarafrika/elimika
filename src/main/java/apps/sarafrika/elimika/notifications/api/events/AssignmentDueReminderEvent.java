package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when an assignment due date is approaching.
 * This triggers email reminders to students about pending submissions.
 */
public record AssignmentDueReminderEvent(
    UUID notificationId,
    UUID recipientId,
    String recipientEmail,
    String recipientName,
    UUID assignmentId,
    String assignmentTitle,
    String courseName,
    LocalDateTime dueDate,
    int daysUntilDue,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public AssignmentDueReminderEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.ASSIGNMENT_DUE_REMINDER;
    }
    
    @Override
    public NotificationPriority getPriority() {
        // Higher priority as due date approaches
        return switch (daysUntilDue) {
            case 0 -> NotificationPriority.CRITICAL;  // Due today
            case 1 -> NotificationPriority.HIGH;     // Due tomorrow
            default -> NotificationPriority.NORMAL;  // 3+ days
        };
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
        variables.put("dueDate", dueDate);
        variables.put("daysUntilDue", daysUntilDue);
        variables.put("isUrgent", daysUntilDue <= 1);
        variables.put("assignmentId", assignmentId);
        return variables;
    }
    
    @Override
    public boolean isBatchable() {
        return daysUntilDue > 1; // Only batch non-urgent reminders
    }
}