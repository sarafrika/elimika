package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Provides instructor compliance analytics to other modules.
 */
public interface InstructorAnalyticsService {

    /**
     * Captures a snapshot of instructor verification metrics.
     *
     * @return immutable instructor analytics snapshot
     */
    InstructorAnalyticsSnapshot captureSnapshot();
}