package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Exposes course creator verification analytics.
 */
public interface CourseCreatorAnalyticsService {

    /**
     * Captures verification state metrics for course creators.
     *
     * @return immutable course creator analytics snapshot
     */
    CourseCreatorAnalyticsSnapshot captureSnapshot();
}