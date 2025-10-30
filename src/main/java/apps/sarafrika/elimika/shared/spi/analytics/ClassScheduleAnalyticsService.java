package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Exposes aggregated analytics for class-level assessment scheduling.
 */
public interface ClassScheduleAnalyticsService {

    /**
     * Captures a snapshot of current scheduling metrics.
     *
     * @return immutable analytics snapshot
     */
    ClassScheduleAnalyticsSnapshot captureSnapshot();
}
