package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Snapshot of course creator verification analytics.
 *
 * @param totalCourseCreators total registered course creators
 * @param verifiedCourseCreators verified course creators
 * @param pendingCourseCreators course creators pending verification
 */
public record CourseCreatorAnalyticsSnapshot(
        long totalCourseCreators,
        long verifiedCourseCreators,
        long pendingCourseCreators
) {
}