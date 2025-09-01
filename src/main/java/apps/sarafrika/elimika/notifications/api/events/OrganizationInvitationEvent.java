package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when an organization invitation is sent to a user.
 * This replaces the direct EmailUtility.sendOrganizationInvitation() call.
 */
public record OrganizationInvitationEvent(
    UUID notificationId,
    UUID recipientId,
    String recipientEmail,
    String recipientName,
    String organizationName,
    String organizationDomain,
    String domainName,
    String branchName,
    String inviterName,
    String invitationToken,
    String notes,
    UUID organizationId,
    LocalDateTime createdAt
) implements NotificationEvent {
    
    public OrganizationInvitationEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @Override
    public NotificationType getNotificationType() {
        return NotificationType.USER_INVITATION_SENT;
    }
    
    @Override
    public NotificationPriority getPriority() {
        return NotificationPriority.HIGH; // Invitations are important
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
        variables.put("organizationDomain", organizationDomain);
        variables.put("roleName", formatRoleName(domainName));
        variables.put("branchName", branchName);
        variables.put("inviterName", inviterName);
        variables.put("notes", notes);
        variables.put("acceptUrl", buildAcceptInvitationUrl(invitationToken));
        variables.put("declineUrl", buildDeclineInvitationUrl(invitationToken));
        return variables;
    }
    
    private String formatRoleName(String domainName) {
        if (domainName == null) return "Member";
        
        return switch (domainName.toLowerCase()) {
            case "student" -> "Student";
            case "instructor" -> "Instructor";
            case "admin" -> "Administrator";
            case "organisation_user" -> "Organization Member";
            default -> "Member";
        };
    }
    
    private String buildAcceptInvitationUrl(String token) {
        return "/invitations/accept?token=" + token;
    }
    
    private String buildDeclineInvitationUrl(String token) {
        return "/invitations/decline?token=" + token;
    }
}