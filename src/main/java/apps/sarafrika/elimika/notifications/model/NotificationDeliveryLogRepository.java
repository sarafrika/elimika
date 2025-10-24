package apps.sarafrika.elimika.notifications.model;

import apps.sarafrika.elimika.notifications.api.DeliveryStatus;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {
    
    Optional<NotificationDeliveryLog> findByNotificationId(UUID notificationId);
    
    List<NotificationDeliveryLog> findByUserUuidOrderByCreatedDateDesc(UUID userUuid);
    
    List<NotificationDeliveryLog> findByDeliveryStatusAndRetryCountLessThan(DeliveryStatus status, int maxRetries);
    
    @Query("SELECT l FROM NotificationDeliveryLog l WHERE l.deliveryStatus = :status " +
           "AND l.retryCount < :maxRetries AND l.createdDate > :after")
    List<NotificationDeliveryLog> findFailedNotificationsForRetry(@Param("status") DeliveryStatus status,
                                                                 @Param("maxRetries") int maxRetries,
                                                                 @Param("after") LocalDateTime after);
    
    @Query("SELECT COUNT(l) FROM NotificationDeliveryLog l WHERE l.userUuid = :userUuid " +
           "AND l.notificationType = :type AND l.deliveryStatus = 'DELIVERED' " +
           "AND l.deliveredAt BETWEEN :start AND :end")
    long countDeliveredNotifications(@Param("userUuid") UUID userUuid,
                                   @Param("type") NotificationType type,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    long countByCreatedDateAfter(LocalDateTime createdAfter);

    long countByDeliveryStatus(DeliveryStatus deliveryStatus);

    long countByDeliveryStatusAndCreatedDateAfter(DeliveryStatus deliveryStatus, LocalDateTime createdAfter);
}
