package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when an organization is successfully registered.
 * This sends a confirmation email to the organization admin.
 */
public record OrganizationRegistrationSuccessEvent(
    UUID notificationId,
    UUID recipientId,
    String recipientEmail,
    String recipientName,
    String organizationName,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public OrganizationRegistrationSuccessEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.ACCOUNT_CREATED; // Reuse existing type
    }
    
    @Override
    public NotificationPriority getPriority() {
        return NotificationPriority.HIGH;
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
        variables.put("userName", recipientName);
        variables.put("organisationName", organizationName);
        return variables;
    }
}