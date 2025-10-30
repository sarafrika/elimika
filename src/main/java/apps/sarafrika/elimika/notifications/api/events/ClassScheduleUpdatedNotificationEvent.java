package apps.sarafrika.elimika.notifications.api.events;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.classes.ClassAssessmentScheduleChangeType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Notification event sent to instructors when a class assessment schedule changes.
 */
public record ClassScheduleUpdatedNotificationEvent(
        UUID notificationId,
        UUID recipientId,
        String recipientEmail,
        String recipientName,
        UUID classDefinitionUuid,
        UUID scheduleUuid,
        UUID lessonUuid,
        UUID assessmentUuid,
        String assessmentTitle,
        String assessmentType,
        ClassAssessmentScheduleChangeType changeType,
        LocalDateTime visibleAt,
        LocalDateTime dueAt,
        String releaseStrategy,
        String timezone,
        String notes,
        String changedBy,
        LocalDateTime changedAt,
        LocalDateTime createdAt
) implements NotificationEvent {

    public ClassScheduleUpdatedNotificationEvent {
        if (notificationId == null) {
            notificationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.CLASS_SCHEDULE_UPDATED;
    }

    @Override
    public NotificationPriority getPriority() {
        if (dueAt == null) {
            return NotificationPriority.NORMAL;
        }
        long hoursUntilDue = java.time.Duration.between(LocalDateTime.now(), dueAt).toHours();
        if (hoursUntilDue <= 24) {
            return NotificationPriority.HIGH;
        }
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
    public Map<String, Object> getTemplateVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("classDefinitionUuid", classDefinitionUuid);
        vars.put("scheduleUuid", scheduleUuid);
        vars.put("lessonUuid", lessonUuid);
        vars.put("assessmentUuid", assessmentUuid);
        vars.put("assessmentTitle", assessmentTitle);
        vars.put("assessmentType", assessmentType);
        vars.put("changeType", changeType != null ? changeType.name() : null);
        vars.put("changeTypeLabel", toChangeLabel(changeType));
        vars.put("visibleAt", visibleAt);
        vars.put("dueAt", dueAt);
        vars.put("releaseStrategy", releaseStrategy);
        vars.put("timezone", timezone);
        vars.put("notes", notes);
        vars.put("changedBy", changedBy);
        vars.put("changedAt", changedAt);
        return vars;
    }

    private String toChangeLabel(ClassAssessmentScheduleChangeType type) {
        if (type == null) {
            return "updated";
        }
        return switch (type) {
            case CREATED -> "created";
            case UPDATED -> "updated";
            case DELETED -> "removed";
        };
    }
}
