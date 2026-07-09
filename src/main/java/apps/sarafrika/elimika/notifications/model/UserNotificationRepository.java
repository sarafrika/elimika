package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long>, JpaSpecificationExecutor<UserNotification> {

    Optional<UserNotification> findByUuidAndRecipientUuid(UUID uuid, UUID recipientUuid);

    Optional<UserNotification> findByRecipientUuidAndDedupeKey(UUID recipientUuid, String dedupeKey);

    Page<UserNotification> findByRecipientUuid(UUID recipientUuid, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE UserNotification n
               SET n.status = :readStatus,
                   n.readAt = COALESCE(n.readAt, :readAt)
             WHERE n.recipientUuid = :recipientUuid
               AND n.status = :unreadStatus
               AND (:domain IS NULL OR n.recipientDomain = :domain OR n.recipientDomain IS NULL)
               AND (:type IS NULL OR n.notificationType = :type)
               AND (:presentation IS NULL OR n.presentation = :presentation)
            """)
    int markUnreadAsRead(
            @Param("recipientUuid") UUID recipientUuid,
            @Param("domain") String domain,
            @Param("type") NotificationType type,
            @Param("presentation") NotificationPresentation presentation,
            @Param("unreadStatus") UserNotificationStatus unreadStatus,
            @Param("readStatus") UserNotificationStatus readStatus,
            @Param("readAt") LocalDateTime readAt
    );
}
