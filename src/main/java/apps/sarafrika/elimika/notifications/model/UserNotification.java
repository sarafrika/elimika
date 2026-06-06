package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.notifications.api.NotificationCategory;
import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import apps.sarafrika.elimika.notifications.util.converter.NotificationCategoryConverter;
import apps.sarafrika.elimika.notifications.util.converter.NotificationPresentationConverter;
import apps.sarafrika.elimika.notifications.util.converter.NotificationPriorityConverter;
import apps.sarafrika.elimika.notifications.util.converter.NotificationTypeConverter;
import apps.sarafrika.elimika.notifications.util.converter.UserNotificationStatusConverter;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notifications")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserNotification extends BaseEntity {

    @Column(name = "recipient_uuid")
    private UUID recipientUuid;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "notification_type")
    @Convert(converter = NotificationTypeConverter.class)
    private NotificationType notificationType;

    @Column(name = "category")
    @Convert(converter = NotificationCategoryConverter.class)
    private NotificationCategory category;

    @Column(name = "priority")
    @Convert(converter = NotificationPriorityConverter.class)
    private NotificationPriority priority;

    @Column(name = "presentation")
    @Convert(converter = NotificationPresentationConverter.class)
    private NotificationPresentation presentation;

    @Column(name = "status")
    @Convert(converter = UserNotificationStatusConverter.class)
    private UserNotificationStatus status;

    @Column(name = "title")
    private String title;

    @Column(name = "body")
    private String body;

    @Column(name = "action_url")
    private String actionUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "popup_seen_at")
    private LocalDateTime popupSeenAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    public static UserNotification create(
            UUID recipientUuid,
            UUID notificationId,
            NotificationType notificationType,
            NotificationPriority priority,
            NotificationPresentation presentation,
            String title,
            String body,
            String actionUrl,
            String metadataJson,
            String dedupeKey,
            LocalDateTime occurredAt
    ) {
        UserNotification notification = new UserNotification();
        notification.setRecipientUuid(recipientUuid);
        notification.setNotificationId(notificationId);
        notification.setNotificationType(notificationType);
        notification.setCategory(notificationType.getCategory());
        notification.setPriority(priority);
        notification.setPresentation(presentation);
        notification.setStatus(UserNotificationStatus.UNREAD);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setActionUrl(actionUrl);
        notification.setMetadataJson(metadataJson);
        notification.setDedupeKey(dedupeKey);
        notification.setOccurredAt(occurredAt);
        return notification;
    }

    public void markRead(LocalDateTime now) {
        if (status != UserNotificationStatus.ARCHIVED) {
            status = UserNotificationStatus.READ;
            readAt = readAt == null ? now : readAt;
        }
    }

    public void markArchived(LocalDateTime now) {
        status = UserNotificationStatus.ARCHIVED;
        archivedAt = archivedAt == null ? now : archivedAt;
    }

    public void markPopupSeen(LocalDateTime now) {
        popupSeenAt = popupSeenAt == null ? now : popupSeenAt;
    }
}
