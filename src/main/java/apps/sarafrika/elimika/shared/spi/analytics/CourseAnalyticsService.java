package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Exposes aggregated course and program analytics for administrative consumers.
 */
public interface CourseAnalyticsService {

    /**
     * Captures a read-only snapshot of course and learning analytics.
     *
     * @return immutable analytics snapshot
     */
    CourseAnalyticsSnapshot captureSnapshot();
}