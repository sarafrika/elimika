package apps.sarafrika.elimika.notifications.service.impl;

import apps.sarafrika.elimika.notifications.api.DeliveryStatus;
import apps.sarafrika.elimika.notifications.model.NotificationDeliveryLogRepository;
import apps.sarafrika.elimika.shared.spi.analytics.NotificationAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.NotificationAnalyticsSnapshot;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationAnalyticsServiceImpl implements NotificationAnalyticsService {

    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;

    @Override
    public NotificationAnalyticsSnapshot captureSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        long createdLast7Days = notificationDeliveryLogRepository.countByCreatedDateAfter(sevenDaysAgo);
        long deliveredLast7Days = notificationDeliveryLogRepository
                .countByDeliveryStatusAndCreatedDateAfter(DeliveryStatus.DELIVERED, sevenDaysAgo);
        long failedLast7Days = notificationDeliveryLogRepository
                .countByDeliveryStatusAndCreatedDateAfter(DeliveryStatus.FAILED, sevenDaysAgo);
        long pendingNotifications = notificationDeliveryLogRepository.countByDeliveryStatus(DeliveryStatus.PENDING);

        return new NotificationAnalyticsSnapshot(
                createdLast7Days,
                deliveredLast7Days,
                failedLast7Days,
                pendingNotifications
        );
    }
}
