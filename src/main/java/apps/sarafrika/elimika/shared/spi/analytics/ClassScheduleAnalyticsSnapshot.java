package apps.sarafrika.elimika.shared.spi.analytics;

import java.time.LocalDateTime;

/**
 * Immutable snapshot of class assessment scheduling analytics metrics.
 */
public record ClassScheduleAnalyticsSnapshot(
        long totalAssignmentSchedules,
        long totalQuizSchedules,
        long upcomingAssignmentsWithin7Days,
        long upcomingQuizzesWithin7Days,
        long overdueAssignments,
        long overdueQuizzes,
        LocalDateTime lastScheduleChange
) {
}
