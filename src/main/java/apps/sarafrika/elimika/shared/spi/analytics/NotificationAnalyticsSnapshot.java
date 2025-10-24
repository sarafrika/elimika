package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Snapshot of notification delivery analytics.
 *
 * @param notificationsCreated7d notifications created in the last 7 days
 * @param notificationsDelivered7d notifications delivered in the last 7 days
 * @param notificationsFailed7d notifications failed in the last 7 days
 * @param pendingNotifications notifications still pending delivery
 */
public record NotificationAnalyticsSnapshot(
        long notificationsCreated7d,
        long notificationsDelivered7d,
        long notificationsFailed7d,
        long pendingNotifications
) {
}