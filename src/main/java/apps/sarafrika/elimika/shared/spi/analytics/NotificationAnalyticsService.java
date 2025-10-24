package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Exposes notification delivery analytics to other modules.
 */
public interface NotificationAnalyticsService {

    /**
     * Captures delivery performance metrics for administrative dashboards.
     *
     * @return immutable notification analytics snapshot
     */
    NotificationAnalyticsSnapshot captureSnapshot();
}