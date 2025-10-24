package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Snapshot of course and learning analytics exposed to other modules.
 */
public record CourseAnalyticsSnapshot(
        long totalCourses,
        long publishedCourses,
        long inReviewCourses,
        long draftCourses,
        long archivedCourses,
        long totalCourseEnrollments,
        long activeCourseEnrollments,
        long newCourseEnrollments7d,
        long completedCourseEnrollments30d,
        double averageCourseProgress,
        long totalTrainingPrograms,
        long publishedTrainingPrograms,
        long activeTrainingPrograms,
        long programEnrollments,
        long completedProgramEnrollments30d
) {
}