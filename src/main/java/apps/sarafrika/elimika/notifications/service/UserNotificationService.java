package apps.sarafrika.elimika.notifications.service;

import apps.sarafrika.elimika.notifications.api.NotificationEvent;
import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import apps.sarafrika.elimika.notifications.dto.NotificationActionResultDTO;
import apps.sarafrika.elimika.notifications.dto.NotificationCountsDTO;
import apps.sarafrika.elimika.notifications.dto.NotificationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserNotificationService {
    NotificationDTO createFromEvent(NotificationEvent event);

    Page<NotificationDTO> listNotifications(
            UUID recipientUuid,
            UserNotificationStatus status,
            NotificationPresentation presentation,
            NotificationType type,
            Boolean popupSeen,
            Pageable pageable
    );

    NotificationCountsDTO getCounts(UUID recipientUuid);

    NotificationDTO applyAction(UUID recipientUuid, UUID notificationUuid, String action);

    NotificationActionResultDTO applyBulkAction(
            UUID recipientUuid,
            String action,
            UserNotificationStatus status,
            NotificationPresentation presentation,
            NotificationType type
    );
}
