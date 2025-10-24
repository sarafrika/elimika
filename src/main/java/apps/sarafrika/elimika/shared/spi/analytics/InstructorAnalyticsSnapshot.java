package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Snapshot of instructor compliance analytics.
 *
 * @param verifiedInstructors number of verified instructors
 * @param pendingInstructors instructors pending verification
 * @param documentsPendingVerification documentation awaiting review
 * @param documentsExpiring30d documents expiring within the next 30 days
 */
public record InstructorAnalyticsSnapshot(
        long verifiedInstructors,
        long pendingInstructors,
        long documentsPendingVerification,
        long documentsExpiring30d
) {
}