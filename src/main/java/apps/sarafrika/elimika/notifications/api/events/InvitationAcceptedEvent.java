package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a user accepts an invitation.
 * This sends a confirmation email to the user.
 */
public record InvitationAcceptedEvent(
    UUID notificationId,
    UUID recipientId,
    String recipientEmail,
    String recipientName,
    String organizationName,
    String branchName,
    String roleName,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public InvitationAcceptedEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.INVITATION_ACCEPTED;
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
        variables.put("organizationName", organizationName);
        variables.put("branchName", branchName);
        variables.put("roleName", formatRoleName(roleName));
        variables.put("loginUrl", "/login");
        return variables;
    }
    
    private String formatRoleName(String roleName) {
        if (roleName == null) return "Member";
        
        return switch (roleName.toLowerCase()) {
            case "student" -> "Student";
            case "instructor" -> "Instructor";
            case "admin" -> "Administrator";
            case "organisation_user" -> "Organization Member";
            default -> "Member";
        };
    }
}