package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a student submits an assignment.
 * This notifies the instructor that a new submission is ready for grading.
 */
public record NewAssignmentSubmissionEvent(
    UUID notificationId,
    UUID recipientId,         // Instructor ID
    String recipientEmail,    // Instructor email
    String recipientName,     // Instructor name
    UUID assignmentId,
    String assignmentTitle,
    String courseName,
    UUID submissionId,
    String studentName,
    String studentEmail,
    LocalDateTime submittedAt,
    LocalDateTime dueDate,
    String submissionText,
    List<String> attachmentFileNames,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public NewAssignmentSubmissionEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.NEW_ASSIGNMENT_SUBMISSION;
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
        variables.put("assignmentId", assignmentId);
        variables.put("submissionId", submissionId);
        variables.put("studentName", studentName);
        variables.put("studentEmail", studentEmail);
        variables.put("submittedAt", submittedAt);
        variables.put("dueDate", dueDate);
        variables.put("submissionText", submissionText);
        variables.put("attachmentFileNames", attachmentFileNames);
        variables.put("hasAttachments", attachmentFileNames != null && !attachmentFileNames.isEmpty());
        variables.put("isLate", dueDate != null && submittedAt.isAfter(dueDate));
        return variables;
    }
    
    @Override
    public boolean isBatchable() {
        return true; // Instructor notifications can be batched
    }
}