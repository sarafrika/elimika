package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when a user declines an invitation.
 * This notifies the organization admin about the declined invitation.
 */
public record InvitationDeclinedEvent(
    UUID notificationId,
    UUID recipientId,           // Admin who sent the invitation
    String recipientEmail,      // Admin email
    String recipientName,       // Admin name
    String declinedUserName,
    String declinedUserEmail,
    String organizationName,
    String branchName,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public InvitationDeclinedEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.INVITATION_DECLINED;
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
        variables.put("adminName", recipientName);
        variables.put("declinedUserName", declinedUserName);
        variables.put("declinedUserEmail", declinedUserEmail);
        variables.put("organizationName", organizationName);
        variables.put("branchName", branchName);
        return variables;
    }
}